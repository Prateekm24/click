using System.IO;
using System.Windows;
using System.Windows.Forms;
using RemoteHost.Services;
using RemoteHost.ViewModels;
using Application = System.Windows.Application;

namespace RemoteHost;

public partial class App : Application
{
    private NotifyIcon? _notifyIcon;
    private ToolStripMenuItem? _pauseMenuItem;

    private HostConfig _config = null!;
    private AudioController _audio = null!;
    private BrightnessController _brightness = null!;
    private AppLauncher _appLauncher = null!;
    private MessageLog _log = null!;
    private WebSocketServer _server = null!;
    private MainViewModel _viewModel = null!;
    private MainWindow? _mainWindow;

    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);

        _config = HostConfig.Load();
        _log = new MessageLog();
        _audio = new AudioController();
        _brightness = new BrightnessController();
        _appLauncher = new AppLauncher(_config);
        _server = new WebSocketServer(_config, _audio, _brightness, _appLauncher, _log);
        _viewModel = new MainViewModel(_config, _server, _audio, _brightness, _appLauncher, _log);

        if (_config.StartOnLogin)
        {
            AutoStartManager.SetEnabled(true);
        }

        _server.Start();
        _viewModel.Listening = _server.IsRunning;

        SetupTrayIcon();

        _mainWindow = new MainWindow(_viewModel);
        _mainWindow.Hide();
    }

    private void SetupTrayIcon()
    {
        _notifyIcon = new NotifyIcon
        {
            Icon = LoadTrayIcon(),
            Visible = true,
            Text = "Remote Host",
        };

        var menu = new ContextMenuStrip();

        var showQrItem = new ToolStripMenuItem("Show pairing QR");
        showQrItem.Click += (_, _) => ShowMainWindow("Pairing");
        menu.Items.Add(showQrItem);

        var appsItem = new ToolStripMenuItem("Launchables…");
        appsItem.Click += (_, _) => ShowMainWindow("Launchables");
        menu.Items.Add(appsItem);

        _pauseMenuItem = new ToolStripMenuItem(_server.IsRunning ? "Pause listening" : "Resume listening");
        _pauseMenuItem.Click += (_, _) =>
        {
            _viewModel.ToggleListening();
            _pauseMenuItem!.Text = _server.IsRunning ? "Pause listening" : "Resume listening";
        };
        menu.Items.Add(_pauseMenuItem);

        menu.Items.Add(new ToolStripSeparator());

        var exitItem = new ToolStripMenuItem("Exit");
        exitItem.Click += (_, _) => ExitApplication();
        menu.Items.Add(exitItem);

        _notifyIcon.ContextMenuStrip = menu;
        _notifyIcon.MouseClick += (_, args) =>
        {
            if (args.Button == System.Windows.Forms.MouseButtons.Left)
            {
                ShowMainWindow("Overview");
            }
        };
    }

    private static System.Drawing.Icon LoadTrayIcon()
    {
        try
        {
            var path = Path.Combine(AppContext.BaseDirectory, "Assets", "tray.ico");
            if (File.Exists(path))
            {
                return new System.Drawing.Icon(path);
            }
        }
        catch
        {
            // Fall back to the system icon below.
        }
        return System.Drawing.SystemIcons.Application;
    }

    private void ShowMainWindow(string pane)
    {
        if (_mainWindow is null)
        {
            return;
        }

        _mainWindow.NavigateTo(pane);
        _mainWindow.Show();
        if (_mainWindow.WindowState == WindowState.Minimized)
        {
            _mainWindow.WindowState = WindowState.Normal;
        }
        _mainWindow.Activate();
    }

    public void ExitApplication()
    {
        _server.Stop();
        if (_notifyIcon != null)
        {
            _notifyIcon.Visible = false;
            _notifyIcon.Dispose();
        }
        Shutdown();
    }

    protected override void OnExit(ExitEventArgs e)
    {
        _audio?.Dispose();
        base.OnExit(e);
    }
}
