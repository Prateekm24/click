using System.Text.Json;
using System.Text.Json.Serialization;

namespace RemoteHost.Models;

// All Host -> Client messages and the pairing payload use exactly the field names from
// PROTOCOL.md, which are lowercase single words, so CamelCase policy maps 1:1
// (Type -> type, Value -> value, For -> for, ...). Incoming Client -> Host messages are
// parsed directly against JsonDocument in WebSocketServer rather than through POCOs,
// since their shape varies per "type"/"action" pair.
public static class ProtocolJson
{
    public static readonly JsonSerializerOptions Options = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
    };
}

public sealed class AuthOkMessage
{
    public string Type { get; } = "auth_ok";
    public string Name { get; set; } = string.Empty;
}

public sealed class AuthErrorMessage
{
    public string Type { get; } = "auth_error";
    public string Message { get; set; } = string.Empty;
}

public sealed class AckMessage
{
    public string Type { get; } = "ack";
    public string For { get; set; } = string.Empty;
}

public sealed class ErrorMessage
{
    public string Type { get; } = "error";
    public string Message { get; set; } = string.Empty;
}

public sealed class StateVolumeMessage
{
    public string Type { get; } = "state";
    public string Key { get; } = "volume";
    public int Value { get; set; }
    public bool Muted { get; set; }
}

public sealed class StateBrightnessMessage
{
    public string Type { get; } = "state";
    public string Key { get; } = "brightness";
    public int Value { get; set; }
}

public sealed class AppItem
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string Tag { get; set; } = string.Empty;
}

public sealed class AppsPushMessage
{
    public string Type { get; } = "apps";
    public List<AppItem> Items { get; set; } = new();
}

public sealed class PongMessage
{
    public string Type { get; } = "pong";
}

public sealed class PairingPayload
{
    public int V { get; set; } = 1;
    public string Ip { get; set; } = string.Empty;
    public int Port { get; set; }
    public string Token { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
}
