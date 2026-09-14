using System.ComponentModel;
using System.IO;
using System.Windows;
using System.Windows.Threading;
using Microsoft.Win32;
using RemoteHost.ViewModels;

namespace RemoteHost;

public partial class MainWindow : Window
{
    private readonly MainViewModel _viewModel;

    // Drives the "LIVE" ticker's continuous horizontal scroll. A DispatcherTimer nudging a
    // TranslateTransform was chosen over a Storyboard because the ticker's content width
    // changes as real traffic arrives (TickerItems is rebuilt from the live message log),
    // and a timer can just re-read ActualWidth on every tick instead of needing the
    // animation's To/By values recomputed and restarted each time the content changes.
    private readonly DispatcherTimer _tickerTimer;
    private const double TickerPixelsPerTick = 1.1;

    public MainWindow(MainViewModel viewModel)
    {
        InitializeComponent();
        _viewModel = viewModel;
        DataContext = _viewModel;
        NavigateTo(_viewModel.SelectedPane);

        _tickerTimer = new DispatcherTimer(DispatcherPriority.Render) { Interval = TimeSpan.FromMilliseconds(30) };
        _tickerTimer.Tick += TickerTimer_Tick;

        // Pause the ticker while the window is hidden in the tray so it isn't animating
        // (and repainting) in the background for no visible benefit.
        IsVisibleChanged += (_, e) =>
        {
            if (e.NewValue is true)
            {
                _tickerTimer.Start();
            }
            else
            {
                _tickerTimer.Stop();
            }
        };
    }

    private void TickerTimer_Tick(object? sender, EventArgs e)
    {
        var loopWidth = TickerItemsControl.ActualWidth / 2;
        if (loopWidth <= 0)
        {
            return;
        }

        var newX = TickerTransform.X - TickerPixelsPerTick;
        if (-newX >= loopWidth)
        {
            // TickerItems is the recent-traffic list concatenated with itself, so wrapping
            // back to 0 exactly here is seamless — the second half now lines up where the
            // first half started.
            newX = 0;
        }
        TickerTransform.X = newX;
    }

    public void NavigateTo(string pane)
    {
        _viewModel.SelectedPane = pane;

        OverviewPanel.Visibility = pane == "Overview" ? Visibility.Visible : Visibility.Collapsed;
        TrafficPanel.Visibility = pane == "Traffic" ? Visibility.Visible : Visibility.Collapsed;
        LaunchablesPanel.Visibility = pane == "Launchables" ? Visibility.Visible : Visibility.Collapsed;
        PairingPanel.Visibility = pane == "Pairing" ? Visibility.Visible : Visibility.Collapsed;
        GeneralPanel.Visibility = pane == "General" ? Visibility.Visible : Visibility.Collapsed;

        NavOverviewButton.Tag = pane == "Overview" ? "Active" : null;
        NavTrafficButton.Tag = pane == "Traffic" ? "Active" : null;
        NavLaunchablesButton.Tag = pane == "Launchables" ? "Active" : null;
        NavPairingButton.Tag = pane == "Pairing" ? "Active" : null;
        NavGeneralButton.Tag = pane == "General" ? "Active" : null;
    }

    private void NavOverview_Click(object sender, RoutedEventArgs e) => NavigateTo("Overview");

    private void NavTraffic_Click(object sender, RoutedEventArgs e) => NavigateTo("Traffic");

    private void NavLaunchables_Click(object sender, RoutedEventArgs e) => NavigateTo("Launchables");

    private void NavPairing_Click(object sender, RoutedEventArgs e) => NavigateTo("Pairing");

    private void NavGeneral_Click(object sender, RoutedEventArgs e) => NavigateTo("General");

    private void Minimize_Click(object sender, RoutedEventArgs e)
    {
        WindowState = WindowState.Minimized;
    }

    private void MaxRestore_Click(object sender, RoutedEventArgs e)
    {
        WindowState = WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized;
    }

    private void Close_Click(object sender, RoutedEventArgs e)
    {
        // Routes through the normal Window.Close() -> OnClosing path below, so the custom
        // titlebar's close glyph respects the same minimize-to-tray-vs-exit setting as
        // clicking the (now hidden) native close button used to.
        Close();
    }

    protected override void OnClosing(CancelEventArgs e)
    {
        e.Cancel = true;
        if (_viewModel.MinimizeToTrayOnClose)
        {
            Hide();
        }
        else
        {
            ((App)System.Windows.Application.Current).ExitApplication();
        }
        base.OnClosing(e);
    }

    private void RotateToken_Click(object sender, RoutedEventArgs e)
    {
        _viewModel.RotateToken();
    }

    private void ToggleListening_Click(object sender, RoutedEventArgs e)
    {
        _viewModel.ToggleListening();
    }

    private void BrowseAppPath_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new Microsoft.Win32.OpenFileDialog
        {
            Filter = "Applications and shortcuts (*.exe;*.lnk)|*.exe;*.lnk|All files (*.*)|*.*",
        };

        if (dialog.ShowDialog() == true)
        {
            AppPathTextBox.Text = dialog.FileName;
            if (string.IsNullOrWhiteSpace(AppNameTextBox.Text))
            {
                AppNameTextBox.Text = Path.GetFileNameWithoutExtension(dialog.FileName);
            }
        }
    }

    private void AddShortcut_Click(object sender, RoutedEventArgs e)
    {
        var name = AppNameTextBox.Text?.Trim();
        var path = AppPathTextBox.Text?.Trim();
        if (string.IsNullOrEmpty(name) || string.IsNullOrEmpty(path))
        {
            return;
        }

        _viewModel.AddShortcut(name, path);
        AppNameTextBox.Text = string.Empty;
        AppPathTextBox.Text = string.Empty;
    }

    private void RemoveShortcut_Click(object sender, RoutedEventArgs e)
    {
        if (sender is System.Windows.Controls.Button { Tag: string id })
        {
            _viewModel.RemoveShortcut(id);
        }
    }
}
