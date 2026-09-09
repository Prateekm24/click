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
}

public sealed class MessageLog
{
    private const int MaxEntries = 50;
    private readonly object _lock = new();
    private readonly LinkedList<LogEntry> _entries = new();

    public event Action<LogEntry>? EntryAdded;

    public void Add(string message, LogLevel level = LogLevel.System)
    {
        var entry = new LogEntry { Message = message, Level = level };
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
