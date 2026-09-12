using System.ComponentModel;
using System.IO;
using System.Windows;
using Microsoft.Win32;
using RemoteHost.ViewModels;

namespace RemoteHost;

public partial class MainWindow : Window
{
    private readonly MainViewModel _viewModel;

    public MainWindow(MainViewModel viewModel)
    {
        InitializeComponent();
        _viewModel = viewModel;
        DataContext = _viewModel;
        NavigateTo(_viewModel.SelectedPane);
    }

    public void NavigateTo(string pane)
    {
        _viewModel.SelectedPane = pane;

        StatusPanel.Visibility = pane == "Status" ? Visibility.Visible : Visibility.Collapsed;
        AppsPanel.Visibility = pane == "Apps" ? Visibility.Visible : Visibility.Collapsed;
        PairingPanel.Visibility = pane == "Pairing" ? Visibility.Visible : Visibility.Collapsed;
        GeneralPanel.Visibility = pane == "General" ? Visibility.Visible : Visibility.Collapsed;

        NavStatusButton.Tag = pane == "Status" ? "Active" : null;
        NavAppsButton.Tag = pane == "Apps" ? "Active" : null;
        NavPairingButton.Tag = pane == "Pairing" ? "Active" : null;
        NavGeneralButton.Tag = pane == "General" ? "Active" : null;
    }

    private void NavStatus_Click(object sender, RoutedEventArgs e) => NavigateTo("Status");

    private void NavApps_Click(object sender, RoutedEventArgs e) => NavigateTo("Apps");

    private void NavPairing_Click(object sender, RoutedEventArgs e) => NavigateTo("Pairing");

    private void NavGeneral_Click(object sender, RoutedEventArgs e) => NavigateTo("General");

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
