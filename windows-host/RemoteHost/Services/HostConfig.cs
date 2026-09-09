using System.IO;
using System.Security.Cryptography;
using System.Text.Json;
using RemoteHost.Models;

namespace RemoteHost.Services;

public sealed class HostConfig
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        WriteIndented = true,
    };

    private static readonly char[] Alphabet =
        "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789".ToCharArray();

    public string Token { get; set; } = GenerateToken();
    public int Port { get; set; } = 8765;
    public List<AppShortcut> AppShortcuts { get; set; } = new();
    public bool StartOnLogin { get; set; }
    public bool MinimizeToTrayOnClose { get; set; } = true;
    public bool AllowInputSimulation { get; set; }

    public static string ConfigDirectory { get; } = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "RemoteHost");

    public static string ConfigPath { get; } = Path.Combine(ConfigDirectory, "config.json");

    public static HostConfig Load()
    {
        try
        {
            if (File.Exists(ConfigPath))
            {
                var json = File.ReadAllText(ConfigPath);
                var config = JsonSerializer.Deserialize<HostConfig>(json, JsonOptions);
                if (config != null)
                {
                    return config;
                }
            }
        }
        catch
        {
            // Fall through and create a fresh config below.
        }

        var fresh = new HostConfig();
        fresh.Save();
        return fresh;
    }

    public void Save()
    {
        Directory.CreateDirectory(ConfigDirectory);
        var json = JsonSerializer.Serialize(this, JsonOptions);
        File.WriteAllText(ConfigPath, json);
    }

    public static string GenerateToken()
    {
        var bytes = RandomNumberGenerator.GetBytes(8);
        Span<char> chars = stackalloc char[9];
        for (var i = 0; i < 4; i++)
        {
            chars[i] = Alphabet[bytes[i] % Alphabet.Length];
        }
        chars[4] = '-';
        for (var i = 0; i < 4; i++)
        {
            chars[5 + i] = Alphabet[bytes[4 + i] % Alphabet.Length];
        }
        return new string(chars);
    }
}
