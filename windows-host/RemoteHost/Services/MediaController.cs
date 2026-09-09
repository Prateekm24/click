using System.Runtime.InteropServices;

namespace RemoteHost.Services;

public static class MediaController
{
    private const byte VkMediaPlayPause = 0xB3;
    private const byte VkMediaNextTrack = 0xB0;
    private const byte VkMediaPrevTrack = 0xB1;
    private const byte VkRight = 0x27;
    private const byte VkLeft = 0x25;
    private const uint KeyEventFExtendedKey = 0x1;
    private const uint KeyEventFKeyUp = 0x2;

    [DllImport("user32.dll")]
    private static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);

    private static void PressKey(byte vk)
    {
        keybd_event(vk, 0, KeyEventFExtendedKey, UIntPtr.Zero);
        keybd_event(vk, 0, KeyEventFExtendedKey | KeyEventFKeyUp, UIntPtr.Zero);
    }

    public static void PlayPause() => PressKey(VkMediaPlayPause);

    public static void Next() => PressKey(VkMediaNextTrack);

    public static void Previous() => PressKey(VkMediaPrevTrack);

    // Windows has no universal media-seek virtual key. This is a best-effort emulation
    // that sends the arrow keys, which many web/video players (browsers, YouTube, etc.)
    // bind to seek when that player has input focus. It does nothing useful in players
    // without such bindings, and does not target a specific window — it goes to whatever
    // currently has focus.
    public static void SeekForward() => PressKey(VkRight);

    public static void SeekBack() => PressKey(VkLeft);
}
