using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Media.Imaging;
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

    public ObservableCollection<LogEntry> LogEntries { get; } = new();
    public ObservableCollection<AppShortcut> AppShortcuts { get; } = new();

    private string _selectedPane = "Status";
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

        foreach (var entry in _log.Snapshot())
        {
            LogEntries.Add(entry);
        }

        _log.EntryAdded += OnLogEntryAdded;
        _audio.VolumeChanged += OnVolumeChanged;
        _server.ClientConnected += OnClientConnected;
        _server.ClientDisconnected += OnClientDisconnected;
        _appLauncher.ShortcutsChanged += OnShortcutsChanged;

        Listening = _server.IsRunning;
        RefreshNetworkInfo();
    }

    public void RefreshNetworkInfo()
    {
        var ip = SubnetGuard.GetPrimaryIPv4();
        LocalIp = ip?.ToString() ?? "unavailable";
        Port = _config.Port;
        RegenerateQr();
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
            LogEntries.Add(entry);
            while (LogEntries.Count > 50)
            {
                LogEntries.RemoveAt(0);
            }
        });
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
        });
    }

    private void OnClientDisconnected()
    {
        RunOnUi(() =>
        {
            IsClientConnected = false;
            ClientStatusText = "No client connected";
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
