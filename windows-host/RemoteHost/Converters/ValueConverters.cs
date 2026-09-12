using System.Globalization;
using System.Windows;
using System.Windows.Data;
using System.Windows.Media;
using RemoteHost.Services;
using Color = System.Windows.Media.Color;

namespace RemoteHost.Converters;

public sealed class ConnectionDotConverter : IValueConverter
{
    private static readonly SolidColorBrush Connected = new(Color.FromRgb(0x7e, 0xe7, 0x87));
    private static readonly SolidColorBrush Disconnected = new(Color.FromRgb(0xf0, 0x70, 0x8f));

    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        => value is bool b && b ? Connected : Disconnected;

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        => throw new NotSupportedException();
}

public sealed class LogLevelToBrushConverter : IValueConverter
{
    private static readonly SolidColorBrush Ok = new(Color.FromRgb(0x7e, 0xe7, 0x87));
    private static readonly SolidColorBrush Warn = new(Color.FromRgb(0xf5, 0xc4, 0x51));
    private static readonly SolidColorBrush Incoming = new(Color.FromRgb(0xc4, 0xb5, 0xfd));
    private static readonly SolidColorBrush Muted = new(Color.FromArgb(0x73, 0xec, 0xe9, 0xf2));

    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        return value switch
        {
            LogLevel.Ok => Ok,
            LogLevel.Warn => Warn,
            LogLevel.Incoming => Incoming,
            _ => Muted,
        };
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        => throw new NotSupportedException();
}

public sealed class InverseBoolToVisibilityConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        => value is bool b && b ? Visibility.Collapsed : Visibility.Visible;

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        => throw new NotSupportedException();
}

public sealed class ListeningLabelConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        => value is bool b && b ? "Pause listening" : "Resume listening";

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        => throw new NotSupportedException();
}
