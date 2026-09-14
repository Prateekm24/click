using System.Globalization;
using System.Windows;
using System.Windows.Data;
using System.Windows.Media;
using RemoteHost.Services;
using Color = System.Windows.Media.Color;

namespace RemoteHost.Converters;

// THEME.md v2: flat near-black palette, single red accent (#e8283f) means "live/active",
// not error. "Lost"/disconnected states use a neutral translucent gray, never red.
public sealed class ConnectionDotConverter : IValueConverter
{
    private static readonly SolidColorBrush Connected = new(Color.FromRgb(0xe8, 0x28, 0x3f));
    private static readonly SolidColorBrush Disconnected = new(Color.FromArgb(0x4d, 0xee, 0xee, 0xef));

    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        => value is bool b && b ? Connected : Disconnected;

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        => throw new NotSupportedException();
}

// Highlights a value in the accent color while a real condition holds (e.g. the stat
// row's CLIENT value while a phone is actually connected), flat OnSurface otherwise.
public sealed class BoolToAccentBrushConverter : IValueConverter
{
    private static readonly SolidColorBrush Accent = new(Color.FromRgb(0xe8, 0x28, 0x3f));
    private static readonly SolidColorBrush Normal = new(Color.FromRgb(0xee, 0xee, 0xef));

    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        => value is bool b && b ? Accent : Normal;

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        => throw new NotSupportedException();
}

public sealed class LogLevelToBrushConverter : IValueConverter
{
    private static readonly SolidColorBrush Incoming = new(Color.FromRgb(0xe8, 0x28, 0x3f));
    private static readonly SolidColorBrush Ok = new(Color.FromArgb(0x99, 0xee, 0xee, 0xef));
    private static readonly SolidColorBrush SystemMuted = new(Color.FromArgb(0x66, 0xee, 0xee, 0xef));
    private static readonly SolidColorBrush Warn = new(Color.FromRgb(0xc9, 0xa2, 0x27));

    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        return value switch
        {
            LogLevel.Incoming => Incoming,
            LogLevel.Ok => Ok,
            LogLevel.Warn => Warn,
            _ => SystemMuted,
        };
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        => throw new NotSupportedException();
}

// Short kind label shown in the Traffic pane's second column ("in"/"ok"/"warn"/"sys"),
// mirroring HostWindowQuiet.dc.html's row.k.
public sealed class LogLevelToLabelConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        return value switch
        {
            LogLevel.Incoming => "in",
            LogLevel.Ok => "ok",
            LogLevel.Warn => "warn",
            _ => "sys",
        };
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        => throw new NotSupportedException();
}

// Traffic pane's right-aligned timing column: a real elapsed-ms figure when the row is a
// request/response pair, an em-dash (matching the mock) for rows that aren't timed.
public sealed class ElapsedMsToStringConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        => value is double ms ? $"{Math.Max(0, Math.Round(ms))} ms" : "—";

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
