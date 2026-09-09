using System.Diagnostics;
using System.Linq;
using System.Text;
using RemoteHost.Models;

namespace RemoteHost.Services;

public sealed class AppLauncher
{
    private readonly HostConfig _config;

    public AppLauncher(HostConfig config)
    {
        _config = config;
    }

    public IReadOnlyList<AppShortcut> Shortcuts => _config.AppShortcuts;

    public event Action? ShortcutsChanged;

    public AppShortcut Add(string name, string path)
    {
        var id = MakeUniqueSlug(name);
        var tag = MakeTag(name);
        var shortcut = new AppShortcut { Id = id, Name = name, Tag = tag, Path = path };
        _config.AppShortcuts.Add(shortcut);
        _config.Save();
        ShortcutsChanged?.Invoke();
        return shortcut;
    }

    public bool Remove(string id)
    {
        var removed = _config.AppShortcuts.RemoveAll(s => s.Id == id) > 0;
        if (removed)
        {
            _config.Save();
            ShortcutsChanged?.Invoke();
        }
        return removed;
    }

    public bool Launch(string id, out string? error)
    {
        var shortcut = _config.AppShortcuts.FirstOrDefault(s => s.Id == id);
        if (shortcut is null)
        {
            error = $"app not found: {id}";
            return false;
        }

        try
        {
            Process.Start(new ProcessStartInfo(shortcut.Path) { UseShellExecute = true });
            error = null;
            return true;
        }
        catch (Exception ex)
        {
            error = $"app not found: {id}";
            _ = ex;
            return false;
        }
    }

    private string MakeUniqueSlug(string name)
    {
        var baseSlug = Slugify(name);
        if (baseSlug.Length == 0)
        {
            baseSlug = "app";
        }

        var slug = baseSlug;
        var n = 2;
        while (_config.AppShortcuts.Any(s => s.Id == slug))
        {
            slug = $"{baseSlug}{n}";
            n++;
        }
        return slug;
    }

    private static string Slugify(string name)
    {
        var sb = new StringBuilder();
        foreach (var c in name.ToLowerInvariant())
        {
            if (char.IsLetterOrDigit(c))
            {
                sb.Append(c);
            }
        }
        return sb.ToString();
    }

    private static string MakeTag(string name)
    {
        var letters = new string(name.Where(char.IsLetter).ToArray());
        if (letters.Length == 0)
        {
            return "??";
        }
        if (letters.Length == 1)
        {
            var single = char.ToUpperInvariant(letters[0]);
            return new string(single, 2);
        }
        return letters.Substring(0, 2).ToUpperInvariant();
    }
}
