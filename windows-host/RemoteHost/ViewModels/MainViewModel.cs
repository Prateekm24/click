using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
using RemoteHost.Models;
using RemoteHost.Services;

namespace RemoteHost.ViewModels;

public sealed class MainViewModel : INotifyPropertyChanged
{
    private readonly HostConfig _config;
    private readonly WebSocketServer _server;
    private readonly AudioController _audio;
    private readonly BrightnessController _brightness;
    private readonly AppLauncher _appLauncher;
    private readonly MessageLog _log;

    public event PropertyChangedEventHandler? PropertyChanged;

    private readonly DispatcherTimer _uptimeTimer;

    // Newest-first (Traffic pane requirement). The live "LIVE" ticker strip is a compact
    // re-render of the same real entries via TickerFormatter, not canned text.
    public ObservableCollection<LogEntry> LogEntries { get; } = new();
    public ObservableCollection<AppShortcut> AppShortcuts { get; } = new();
    public ObservableCollection<string> TickerItems { get; } = new();

    private string _selectedPane = "Overview";
    public string SelectedPane
    {
        get => _selectedPane;
        set => SetField(ref _selectedPane, value);
    }

    private string _clientStatusText = "No client connected";
    public string ClientStatusText
    {
        get => _clientStatusText;
        set => SetField(ref _clientStatusText, value);
    }

    private bool _isClientConnected;
    public bool IsClientConnected
    {
        get => _isClientConnected;
        set => SetField(ref _isClientConnected, value);
    }

    private int _volume;
    public int Volume
    {
        get => _volume;
        set => SetField(ref _volume, value);
    }

    private bool _muted;
    public bool Muted
    {
        get => _muted;
        set => SetField(ref _muted, value);
    }

    private int _brightnessValue;
    public int BrightnessValue
    {
        get => _brightnessValue;
        set => SetField(ref _brightnessValue, value);
    }

    private bool _brightnessSupported = true;
    public bool BrightnessSupported
    {
        get => _brightnessSupported;
        set => SetField(ref _brightnessSupported, value);
    }

    private bool _listening;
    public bool Listening
    {
        get => _listening;
        set => SetField(ref _listening, value);
    }

    private string _localIp = "unavailable";
    public string LocalIp
    {
        get => _localIp;
        set => SetField(ref _localIp, value);
    }

    private int _port;
    public int Port
    {
        get => _port;
        set => SetField(ref _port, value);
    }

    private string _machineName = Environment.MachineName;
    public string MachineName
    {
        get => _machineName;
        set => SetField(ref _machineName, value);
    }

    private string _token = string.Empty;
    public string Token
    {
        get => _token;
        set => SetField(ref _token, value);
    }

    private BitmapImage? _qrImage;
    public BitmapImage? QrImage
    {
        get => _qrImage;
        set => SetField(ref _qrImage, value);
    }

    private bool _startOnLogin;
    public bool StartOnLogin
    {
        get => _startOnLogin;
        set
        {
            if (SetField(ref _startOnLogin, value))
            {
                _config.StartOnLogin = value;
                _config.Save();
                AutoStartManager.SetEnabled(value);
            }
        }
    }

    private bool _minimizeToTrayOnClose;
    public bool MinimizeToTrayOnClose
    {
        get => _minimizeToTrayOnClose;
        set
        {
            if (SetField(ref _minimizeToTrayOnClose, value))
            {
                _config.MinimizeToTrayOnClose = value;
                _config.Save();
            }
        }
    }

    private bool _allowInputSimulation;
    public bool AllowInputSimulation
    {
        get => _allowInputSimulation;
        set
        {
            if (SetField(ref _allowInputSimulation, value))
            {
                _config.AllowInputSimulation = value;
                _config.Save();
            }
        }
    }

    // Stat row (THEME.md: "one real, live-updating view" replacing the mock's fake
    // alternating datasets) — all four values are backed by real WebSocketServer state.
    private int _commandsHandledCount;
    public int CommandsHandledCount
    {
        get => _commandsHandledCount;
        set => SetField(ref _commandsHandledCount, value);
    }

    private int _rejectedCount;
    public int RejectedCount
    {
        get => _rejectedCount;
        set => SetField(ref _rejectedCount, value);
    }

    private string _uptimeText = "0m";
    public string UptimeText
    {
        get => _uptimeText;
        set => SetField(ref _uptimeText, value);
    }

    private string _listeningSummaryText = "PAUSED";
    public string ListeningSummaryText
    {
        get => _listeningSummaryText;
        set => SetField(ref _listeningSummaryText, value);
    }

    private string _wsAddressText = string.Empty;
    public string WsAddressText
    {
        get => _wsAddressText;
        set => SetField(ref _wsAddressText, value);
    }

    public MainViewModel(
        HostConfig config,
        WebSocketServer server,
        AudioController audio,
        BrightnessController brightness,
        AppLauncher appLauncher,
        MessageLog log)
    {
        _config = config;
        _server = server;
        _audio = audio;
        _brightness = brightness;
        _appLauncher = appLauncher;
        _log = log;

        foreach (var shortcut in _appLauncher.Shortcuts)
        {
            AppShortcuts.Add(shortcut);
        }

        _port = config.Port;
        _token = config.Token;
        _startOnLogin = config.StartOnLogin;
        _minimizeToTrayOnClose = config.MinimizeToTrayOnClose;
        _allowInputSimulation = config.AllowInputSimulation;

        _volume = _audio.GetVolume();
        _muted = _audio.GetMute();
        var currentBrightness = _brightness.GetBrightness();
        _brightnessSupported = currentBrightness.HasValue;
        _brightnessValue = currentBrightness ?? 0;

        // MessageLog.Snapshot() is oldest-first; inserting each at index 0, in that same
        // order, leaves LogEntries newest-first — the order the Traffic pane requires.
        foreach (var entry in _log.Snapshot())
        {
            LogEntries.Insert(0, entry);
        }
        RefreshTicker();

        _log.EntryAdded += OnLogEntryAdded;
        _audio.VolumeChanged += OnVolumeChanged;
        _server.ClientConnected += OnClientConnected;
        _server.ClientDisconnected += OnClientDisconnected;
        _server.CommandHandled += OnCommandHandled;
        _server.ConnectionRejected += OnConnectionRejected;
        _appLauncher.ShortcutsChanged += OnShortcutsChanged;

        _commandsHandledCount = _server.CommandsHandled;
        _rejectedCount = _server.RejectedConnections;

        Listening = _server.IsRunning;
        RefreshNetworkInfo();
        RefreshListeningSummary();

        _uptimeTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(1) };
        _uptimeTimer.Tick += (_, _) => RefreshUptime();
        _uptimeTimer.Start();
        RefreshUptime();
    }

    public void RefreshNetworkInfo()
    {
        var ip = SubnetGuard.GetPrimaryIPv4();
        LocalIp = ip?.ToString() ?? "unavailable";
        Port = _config.Port;
        WsAddressText = $"ws://{LocalIp}:{Port}";
        RegenerateQr();
    }

    private void RefreshListeningSummary()
    {
        ListeningSummaryText = Listening
            ? $"LISTENING · {(IsClientConnected ? 1 : 0)} CLIENT"
            : "PAUSED";
    }

    private void RefreshUptime()
    {
        var startedAtUtc = _server.StartedAtUtc;
        if (!startedAtUtc.HasValue)
        {
            UptimeText = "0m";
            return;
        }

        var elapsed = DateTime.UtcNow - startedAtUtc.Value;
        UptimeText = elapsed.TotalHours >= 1
            ? $"{(int)elapsed.TotalHours}h {elapsed.Minutes}m"
            : elapsed.TotalMinutes >= 1
                ? $"{(int)elapsed.TotalMinutes}m"
                : $"{Math.Max(0, (int)elapsed.TotalSeconds)}s";
    }

    private void RefreshTicker()
    {
        // LogEntries is newest-first; take the most recent handful, put them back in
        // chronological order, then double the list so the marquee loop is seamless
        // (mirrors HostWindowQuiet.dc.html's `ticker: t.concat(t)`).
        var recent = LogEntries.Take(14).Select(TickerFormatter.Format).Reverse().ToList();
        if (recent.Count == 0)
        {
            TickerItems.Clear();
            return;
        }

        TickerItems.Clear();
        foreach (var item in recent.Concat(recent))
        {
            TickerItems.Add(item);
        }
    }

    public void RegenerateQr()
    {
        var payload = new PairingPayload
        {
            V = 1,
            Ip = LocalIp,
            Port = Port,
            Token = Token,
            Name = MachineName,
        };

        try
        {
            QrImage = QrCodeGenerator.GeneratePairingQr(payload);
        }
        catch
        {
            // Leave the previous QR image (or none) in place rather than crash the UI.
        }
    }

    public void RotateToken()
    {
        Token = HostConfig.GenerateToken();
        _config.Token = Token;
        _config.Save();
        RegenerateQr();
        _log.Add("Pairing token rotated", LogLevel.System);
    }

    public void ToggleListening()
    {
        if (_server.IsRunning)
        {
            _server.Stop();
        }
        else
        {
            _server.Start();
        }
        Listening = _server.IsRunning;
        RefreshListeningSummary();
        RefreshUptime();
    }

    public void AddShortcut(string name, string path)
    {
        _appLauncher.Add(name, path);
    }

    public void RemoveShortcut(string id)
    {
        _appLauncher.Remove(id);
    }

    private void OnShortcutsChanged()
    {
        RunOnUi(() =>
        {
            AppShortcuts.Clear();
            foreach (var shortcut in _appLauncher.Shortcuts)
            {
                AppShortcuts.Add(shortcut);
            }
        });
    }

    private void OnLogEntryAdded(LogEntry entry)
    {
        RunOnUi(() =>
        {
            LogEntries.Insert(0, entry);
            while (LogEntries.Count > 50)
            {
                LogEntries.RemoveAt(LogEntries.Count - 1);
            }
            RefreshTicker();
        });
    }

    private void OnCommandHandled()
    {
        RunOnUi(() => CommandsHandledCount = _server.CommandsHandled);
    }

    private void OnConnectionRejected()
    {
        RunOnUi(() => RejectedCount = _server.RejectedConnections);
    }

    private void OnVolumeChanged(int value, bool muted)
    {
        RunOnUi(() =>
        {
            Volume = value;
            Muted = muted;
        });
    }

    private void OnClientConnected()
    {
        RunOnUi(() =>
        {
            IsClientConnected = true;
            ClientStatusText = _server.ConnectedClientName ?? "Connected";
            RefreshListeningSummary();
        });
    }

    private void OnClientDisconnected()
    {
        RunOnUi(() =>
        {
            IsClientConnected = false;
            ClientStatusText = "No client connected";
            RefreshListeningSummary();
        });
    }

    private static void RunOnUi(Action action)
    {
        var app = System.Windows.Application.Current;
        var dispatcher = app?.Dispatcher;
        if (dispatcher is null || dispatcher.CheckAccess())
        {
            action();
        }
        else
        {
            dispatcher.BeginInvoke(action);
        }
    }

    private bool SetField<T>(ref T field, T value, [CallerMemberName] string? propertyName = null)
    {
        if (EqualityComparer<T>.Default.Equals(field, value))
        {
            return false;
        }
        field = value;
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        return true;
    }
}
