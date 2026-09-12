using System.IO;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Net.WebSockets;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using RemoteHost.Models;

namespace RemoteHost.Services;

// A raw TcpListener + manual HTTP/1.1 Upgrade handshake is used instead of HttpListener
// because HttpListener requires either a URL ACL reservation (netsh http add urlacl) or
// running elevated when bound to anything other than "localhost" — a bad first-run
// experience for a tray app that is expected to "just work" on 0.0.0.0. Handing the raw
// stream to WebSocket.CreateFromStream after our own 101 response avoids that entirely.
public sealed class WebSocketServer
{
    private const string WebSocketGuid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private readonly HostConfig _config;
    private readonly AudioController _audio;
    private readonly BrightnessController _brightness;
    private readonly AppLauncher _appLauncher;
    private readonly MessageLog _log;

    private readonly object _clientLock = new();
    private TcpListener? _listener;
    private CancellationTokenSource? _serverCts;
    private WebSocket? _currentSocket;
    private CancellationTokenSource? _currentClientCts;

    public bool IsRunning { get; private set; }
    public string? ConnectedClientName { get; private set; }

    public event Action? ClientConnected;
    public event Action? ClientDisconnected;

    public WebSocketServer(
        HostConfig config,
        AudioController audio,
        BrightnessController brightness,
        AppLauncher appLauncher,
        MessageLog log)
    {
        _config = config;
        _audio = audio;
        _brightness = brightness;
        _appLauncher = appLauncher;
        _log = log;

        _audio.VolumeChanged += (value, muted) =>
            Broadcast(new StateVolumeMessage { Value = value, Muted = muted });
        _appLauncher.ShortcutsChanged += () => Broadcast(BuildAppsMessage());
    }

    public void Start()
    {
        if (IsRunning)
        {
            return;
        }

        _serverCts = new CancellationTokenSource();
        _listener = new TcpListener(IPAddress.Any, _config.Port);
        _listener.Start();
        IsRunning = true;
        _log.Add($"Listening on 0.0.0.0:{_config.Port}", LogLevel.System);

        _ = Task.Run(() => AcceptLoopAsync(_serverCts.Token));
    }

    public void Stop()
    {
        if (!IsRunning)
        {
            return;
        }

        IsRunning = false;

        try { _serverCts?.Cancel(); } catch { }
        try { _listener?.Stop(); } catch { }

        lock (_clientLock)
        {
            try { _currentClientCts?.Cancel(); } catch { }
            _currentSocket = null;
            _currentClientCts = null;
        }

        _log.Add("Stopped listening", LogLevel.System);
    }

    private async Task AcceptLoopAsync(CancellationToken token)
    {
        while (!token.IsCancellationRequested)
        {
            TcpClient client;
            try
            {
                client = await _listener!.AcceptTcpClientAsync(token);
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (ObjectDisposedException)
            {
                break;
            }
            catch (SocketException)
            {
                break;
            }

            _ = Task.Run(() => HandleClientAsync(client, token));
        }
    }

    private async Task HandleClientAsync(TcpClient client, CancellationToken serverToken)
    {
        var remoteEndPoint = client.Client.RemoteEndPoint as IPEndPoint;
        try
        {
            if (remoteEndPoint is null || !SubnetGuard.IsAllowed(remoteEndPoint.Address))
            {
                _log.Add($"Rejected connection from {remoteEndPoint} (outside local subnet)", LogLevel.Warn);
                client.Close();
                return;
            }

            using var stream = client.GetStream();
            var secWebSocketKey = await ReadHttpUpgradeRequestAsync(stream, serverToken);
            if (secWebSocketKey is null)
            {
                client.Close();
                return;
            }

            await WriteUpgradeResponseAsync(stream, secWebSocketKey);

            using var socket = WebSocket.CreateFromStream(
                stream, isServer: true, subProtocol: null, keepAliveInterval: TimeSpan.FromSeconds(30));

            await RunClientSessionAsync(socket, remoteEndPoint, serverToken);
        }
        catch (Exception ex)
        {
            _log.Add($"Client error: {ex.Message}", LogLevel.Warn);
        }
        finally
        {
            client.Close();
        }
    }

    private static async Task<string?> ReadHttpUpgradeRequestAsync(NetworkStream stream, CancellationToken token)
    {
        var headerBytes = new List<byte>();
        var buffer = new byte[1];
        var consecutiveNewlines = 0;

        while (headerBytes.Count < 16384)
        {
            var read = await stream.ReadAsync(buffer.AsMemory(0, 1), token);
            if (read == 0)
            {
                return null;
            }

            headerBytes.Add(buffer[0]);

            if (buffer[0] == (byte)'\n')
            {
                consecutiveNewlines++;
                if (consecutiveNewlines >= 2)
                {
                    break;
                }
            }
            else if (buffer[0] != (byte)'\r')
            {
                consecutiveNewlines = 0;
            }
        }

        var text = Encoding.ASCII.GetString(headerBytes.ToArray());
        var lines = text.Split("\r\n", StringSplitOptions.RemoveEmptyEntries);
        if (lines.Length == 0)
        {
            return null;
        }

        string? upgrade = null;
        string? key = null;

        foreach (var line in lines.Skip(1))
        {
            var idx = line.IndexOf(':');
            if (idx < 0)
            {
                continue;
            }

            var name = line[..idx].Trim();
            var value = line[(idx + 1)..].Trim();

            if (string.Equals(name, "Upgrade", StringComparison.OrdinalIgnoreCase))
            {
                upgrade = value;
            }
            else if (string.Equals(name, "Sec-WebSocket-Key", StringComparison.OrdinalIgnoreCase))
            {
                key = value;
            }
        }

        if (key is null)
        {
            return null;
        }
        if (upgrade is null || !upgrade.Contains("websocket", StringComparison.OrdinalIgnoreCase))
        {
            return null;
        }

        return key;
    }

    private static async Task WriteUpgradeResponseAsync(NetworkStream stream, string secWebSocketKey)
    {
        var accept = ComputeAcceptKey(secWebSocketKey);
        var response =
            "HTTP/1.1 101 Switching Protocols\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            $"Sec-WebSocket-Accept: {accept}\r\n\r\n";
        var bytes = Encoding.ASCII.GetBytes(response);
        await stream.WriteAsync(bytes);
        await stream.FlushAsync();
    }

    private static string ComputeAcceptKey(string secWebSocketKey)
    {
        var combined = secWebSocketKey + WebSocketGuid;
        var hash = SHA1.HashData(Encoding.ASCII.GetBytes(combined));
        return Convert.ToBase64String(hash);
    }

    private async Task RunClientSessionAsync(WebSocket socket, IPEndPoint remoteEndPoint, CancellationToken serverToken)
    {
        using var sessionCts = CancellationTokenSource.CreateLinkedTokenSource(serverToken);
        var buffer = new byte[8192];
        var authenticated = false;

        try
        {
            while (socket.State == WebSocketState.Open && !sessionCts.IsCancellationRequested)
            {
                var message = await ReceiveFullMessageAsync(socket, buffer, sessionCts.Token);
                if (message is null)
                {
                    break;
                }

                if (!authenticated)
                {
                    if (!TryHandleAuth(message, out var name, out var errorMsg))
                    {
                        await SendAsync(socket, new AuthErrorMessage { Message = errorMsg ?? "invalid token" }, sessionCts.Token);
                        await CloseWithCodeAsync(socket, 4001, "auth failed");
                        _log.Add($"Auth failed from {remoteEndPoint.Address}", LogLevel.Warn);
                        return;
                    }

                    lock (_clientLock)
                    {
                        // Only one authenticated client at a time: replace whoever was there.
                        _currentClientCts?.Cancel();
                        _currentSocket = socket;
                        _currentClientCts = sessionCts;
                    }

                    authenticated = true;
                    ConnectedClientName = name;
                    ClientConnected?.Invoke();
                    _log.Add($"Client authenticated from {remoteEndPoint.Address}", LogLevel.Ok);

                    await SendAsync(socket, new AuthOkMessage { Name = Environment.MachineName }, sessionCts.Token);
                    await SendAsync(socket, BuildAppsMessage(), sessionCts.Token);
                    await SendAsync(socket, new StateVolumeMessage { Value = _audio.GetVolume(), Muted = _audio.GetMute() }, sessionCts.Token);

                    var brightness = _brightness.GetBrightness();
                    if (brightness.HasValue)
                    {
                        await SendAsync(socket, new StateBrightnessMessage { Value = brightness.Value }, sessionCts.Token);
                    }

                    continue;
                }

                await HandleMessageAsync(socket, message, sessionCts.Token);
            }
        }
        catch (OperationCanceledException)
        {
        }
        catch (WebSocketException)
        {
        }
        finally
        {
            lock (_clientLock)
            {
                if (ReferenceEquals(_currentSocket, socket))
                {
                    _currentSocket = null;
                    _currentClientCts = null;
                }
            }

            if (authenticated)
            {
                ConnectedClientName = null;
                ClientDisconnected?.Invoke();
                _log.Add($"Client disconnected ({remoteEndPoint.Address})", LogLevel.System);
            }

            if (socket.State == WebSocketState.Open)
            {
                try { await socket.CloseAsync(WebSocketCloseStatus.NormalClosure, null, CancellationToken.None); } catch { }
            }
        }
    }

    private static async Task CloseWithCodeAsync(WebSocket socket, int code, string reason)
    {
        try
        {
            await socket.CloseAsync((WebSocketCloseStatus)code, reason, CancellationToken.None);
        }
        catch
        {
            // Best-effort; the TCP connection is torn down by the caller regardless.
        }
    }

    private bool TryHandleAuth(string message, out string? name, out string? error)
    {
        name = null;
        error = null;
        try
        {
            using var doc = JsonDocument.Parse(message);
            var root = doc.RootElement;

            if (!root.TryGetProperty("type", out var typeProp) || typeProp.GetString() != "auth")
            {
                error = "auth required";
                return false;
            }

            if (!root.TryGetProperty("token", out var tokenProp))
            {
                error = "invalid token";
                return false;
            }

            var token = tokenProp.GetString();
            if (string.IsNullOrEmpty(token) || token != _config.Token)
            {
                error = "invalid token";
                return false;
            }

            name = Environment.MachineName;
            return true;
        }
        catch
        {
            error = "invalid token";
            return false;
        }
    }

    private async Task HandleMessageAsync(WebSocket socket, string message, CancellationToken token)
    {
        JsonDocument doc;
        try
        {
            doc = JsonDocument.Parse(message);
        }
        catch
        {
            await SendAsync(socket, new ErrorMessage { Message = "unsupported action" }, token);
            return;
        }

        using (doc)
        {
            var root = doc.RootElement;
            var type = root.TryGetProperty("type", out var t) ? t.GetString() : null;
            _log.Add($"< {message}", LogLevel.Incoming);

            switch (type)
            {
                case "volume":
                    await HandleVolumeAsync(socket, root, token);
                    break;
                case "brightness":
                    await HandleBrightnessAsync(socket, root, token);
                    break;
                case "media":
                    await HandleMediaAsync(socket, root, token);
                    break;
                case "app":
                    await HandleAppAsync(socket, root, token);
                    break;
                case "apps":
                    await SendAsync(socket, BuildAppsMessage(), token);
                    break;
                case "ping":
                    await SendAsync(socket, new PongMessage(), token);
                    break;
                default:
                    await SendAsync(socket, new ErrorMessage { Message = "unsupported action" }, token);
                    break;
            }
        }
    }

    private async Task HandleVolumeAsync(WebSocket socket, JsonElement root, CancellationToken token)
    {
        var action = root.TryGetProperty("action", out var a) ? a.GetString() : null;
        switch (action)
        {
            case "set":
                if (root.TryGetProperty("value", out var v) && v.TryGetInt32(out var value))
                {
                    _audio.SetVolume(value);
                    await SendAsync(socket, new AckMessage { For = "volume" }, token);
                }
                else
                {
                    await SendAsync(socket, new ErrorMessage { Message = "unsupported action" }, token);
                }
                break;
            case "mute":
                _audio.ToggleMute();
                await SendAsync(socket, new AckMessage { For = "volume" }, token);
                break;
            case "get":
                await SendAsync(socket, new StateVolumeMessage { Value = _audio.GetVolume(), Muted = _audio.GetMute() }, token);
                break;
            default:
                await SendAsync(socket, new ErrorMessage { Message = "unsupported action" }, token);
                break;
        }
    }

    private async Task HandleBrightnessAsync(WebSocket socket, JsonElement root, CancellationToken token)
    {
        var action = root.TryGetProperty("action", out var a) ? a.GetString() : null;
        switch (action)
        {
            case "set":
                if (root.TryGetProperty("value", out var v) && v.TryGetInt32(out var value))
                {
                    if (_brightness.SetBrightness(value))
                    {
                        await SendAsync(socket, new AckMessage { For = "brightness" }, token);
                        await SendAsync(socket, new StateBrightnessMessage { Value = value }, token);
                    }
                    else
                    {
                        await SendAsync(socket, new ErrorMessage { Message = "brightness not supported on this display" }, token);
                    }
                }
                else
                {
                    await SendAsync(socket, new ErrorMessage { Message = "unsupported action" }, token);
                }
                break;
            case "get":
                var current = _brightness.GetBrightness();
                if (current.HasValue)
                {
                    await SendAsync(socket, new StateBrightnessMessage { Value = current.Value }, token);
                }
                else
                {
                    await SendAsync(socket, new ErrorMessage { Message = "brightness not supported on this display" }, token);
                }
                break;
            default:
                await SendAsync(socket, new ErrorMessage { Message = "unsupported action" }, token);
                break;
        }
    }

    private async Task HandleMediaAsync(WebSocket socket, JsonElement root, CancellationToken token)
    {
        var action = root.TryGetProperty("action", out var a) ? a.GetString() : null;
        switch (action)
        {
            case "play_pause":
                MediaController.PlayPause();
                break;
            case "next":
                MediaController.Next();
                break;
            case "previous":
                MediaController.Previous();
                break;
            case "seek_forward":
                MediaController.SeekForward();
                break;
            case "seek_back":
                MediaController.SeekBack();
                break;
            default:
                await SendAsync(socket, new ErrorMessage { Message = "unsupported action" }, token);
                return;
        }
        await SendAsync(socket, new AckMessage { For = "media" }, token);
    }

    private async Task HandleAppAsync(WebSocket socket, JsonElement root, CancellationToken token)
    {
        var action = root.TryGetProperty("action", out var a) ? a.GetString() : null;
        if (action != "launch")
        {
            await SendAsync(socket, new ErrorMessage { Message = "unsupported action" }, token);
            return;
        }

        var id = root.TryGetProperty("id", out var idProp) ? idProp.GetString() : null;
        if (string.IsNullOrEmpty(id))
        {
            await SendAsync(socket, new ErrorMessage { Message = "unsupported action" }, token);
            return;
        }

        if (_appLauncher.Launch(id, out var error))
        {
            await SendAsync(socket, new AckMessage { For = "app" }, token);
        }
        else
        {
            await SendAsync(socket, new ErrorMessage { Message = error ?? $"app not found: {id}" }, token);
        }
    }

    private AppsPushMessage BuildAppsMessage()
    {
        return new AppsPushMessage
        {
            Items = _appLauncher.Shortcuts
                .Select(s => new AppItem { Id = s.Id, Name = s.Name, Tag = s.Tag })
                .ToList(),
        };
    }

    private static async Task<string?> ReceiveFullMessageAsync(WebSocket socket, byte[] buffer, CancellationToken token)
    {
        using var ms = new MemoryStream();
        ValueWebSocketReceiveResult result;
        do
        {
            result = await socket.ReceiveAsync(buffer.AsMemory(), token);
            if (result.MessageType == WebSocketMessageType.Close)
            {
                return null;
            }
            ms.Write(buffer, 0, result.Count);
        } while (!result.EndOfMessage);

        return Encoding.UTF8.GetString(ms.ToArray());
    }

    private async Task SendAsync<T>(WebSocket socket, T message, CancellationToken token)
    {
        if (socket.State != WebSocketState.Open)
        {
            return;
        }

        var json = JsonSerializer.Serialize(message, ProtocolJson.Options);
        var bytes = Encoding.UTF8.GetBytes(json);
        try
        {
            // WebSocket.SendAsync has both an ArraySegment<byte> and a ReadOnlyMemory<byte>
            // overload; passing a bare byte[] is ambiguous (CS0121) since both accept it via
            // an implicit conversion. AsMemory() picks the ReadOnlyMemory<byte> one exactly.
            await socket.SendAsync(bytes.AsMemory(), WebSocketMessageType.Text, true, token);
        }
        catch
        {
            // The receive loop will observe the closed/broken socket and clean up.
        }
    }

    private void Broadcast<T>(T message)
    {
        WebSocket? socket;
        CancellationToken token;
        lock (_clientLock)
        {
            socket = _currentSocket;
            token = _currentClientCts?.Token ?? CancellationToken.None;
        }

        if (socket is null)
        {
            return;
        }

        _ = SendAsync(socket, message, token);
    }
}
