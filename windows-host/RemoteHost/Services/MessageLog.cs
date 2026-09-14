namespace RemoteHost.Services;

public enum LogLevel
{
    System,
    Ok,
    Incoming,
    Warn,
}

public sealed class LogEntry
{
    public DateTime Timestamp { get; init; } = DateTime.Now;
    public string Message { get; init; } = string.Empty;
    public LogLevel Level { get; init; }

    // Real, server-measured elapsed time (ms) between receiving a client message and
    // sending its response (ack/error/state/auth_ok). Null for rows that aren't a
    // request/response pair (server-lifecycle notices, subnet rejections, etc.) — the
    // Traffic pane renders those with an em-dash instead of a bogus number.
    public double? ElapsedMs { get; init; }
}

public sealed class MessageLog
{
    private const int MaxEntries = 50;
    private readonly object _lock = new();
    private readonly LinkedList<LogEntry> _entries = new();

    public event Action<LogEntry>? EntryAdded;

    public void Add(string message, LogLevel level = LogLevel.System, double? elapsedMs = null)
    {
        var entry = new LogEntry { Message = message, Level = level, ElapsedMs = elapsedMs };
        lock (_lock)
        {
            _entries.AddLast(entry);
            while (_entries.Count > MaxEntries)
            {
                _entries.RemoveFirst();
            }
        }
        EntryAdded?.Invoke(entry);
    }

    public IReadOnlyList<LogEntry> Snapshot()
    {
        lock (_lock)
        {
            return _entries.ToList();
        }
    }
}
