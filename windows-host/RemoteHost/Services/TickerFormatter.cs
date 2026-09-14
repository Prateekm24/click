using System.Text;
using System.Text.Json;

namespace RemoteHost.Services;

// Turns a real MessageLog entry into a short one-line summary for the Host window's
// "LIVE" ticker strip (THEME.md / HostWindowQuiet.dc.html) — e.g. "volume set 45 · 12 ms".
// The ticker must be fed by real recent traffic, not canned text, so this is just a
// compact renderer over the same LogEntry data shown in the Traffic pane.
public static class TickerFormatter
{
    public static string Format(LogEntry entry)
    {
        var summary = Summarize(entry.Message);
        return entry.ElapsedMs.HasValue
            ? $"{summary} · {FormatMs(entry.ElapsedMs.Value)}"
            : summary;
    }

    private static string FormatMs(double ms) => $"{Math.Max(0, Math.Round(ms))} ms";

    private static string Summarize(string message)
    {
        var trimmed = message.TrimStart();
        if (trimmed.StartsWith('{'))
        {
            try
            {
                using var doc = JsonDocument.Parse(trimmed);
                var root = doc.RootElement;
                var parts = new StringBuilder();

                void Append(string? value)
                {
                    if (string.IsNullOrEmpty(value))
                    {
                        return;
                    }
                    if (parts.Length > 0)
                    {
                        parts.Append(' ');
                    }
                    parts.Append(value);
                }

                Append(root.TryGetProperty("type", out var typeProp) ? typeProp.GetString() : null);
                Append(root.TryGetProperty("action", out var actionProp) ? actionProp.GetString() : null);
                if (root.TryGetProperty("value", out var valueProp))
                {
                    Append(valueProp.ToString());
                }
                Append(root.TryGetProperty("id", out var idProp) ? idProp.GetString() : null);

                if (parts.Length > 0)
                {
                    return parts.ToString();
                }
            }
            catch (JsonException)
            {
                // Not (purely) JSON — e.g. the "{...} -> auth_ok" combined auth log line.
                // Fall through to the plain-text truncation below.
            }
        }

        const int maxLength = 46;
        return message.Length > maxLength ? message[..maxLength] + "…" : message;
    }
}
