using NAudio.CoreAudioApi;

namespace RemoteHost.Services;

public sealed class AudioController : IDisposable
{
    private readonly MMDeviceEnumerator _enumerator = new();
    private MMDevice? _device;

    public event Action<int, bool>? VolumeChanged;

    public AudioController()
    {
        TryBindDevice();
    }

    private void TryBindDevice()
    {
        try
        {
            if (_device != null)
            {
                _device.AudioEndpointVolume.OnVolumeNotification -= OnVolumeNotification;
                _device.Dispose();
            }
            _device = _enumerator.GetDefaultAudioEndpoint(DataFlow.Render, Role.Multimedia);
            _device.AudioEndpointVolume.OnVolumeNotification += OnVolumeNotification;
        }
        catch
        {
            _device = null;
        }
    }

    private void OnVolumeNotification(AudioVolumeNotificationData data)
    {
        VolumeChanged?.Invoke((int)Math.Round(data.MasterVolume * 100), data.Muted);
    }

    public int GetVolume()
    {
        if (_device is null)
        {
            TryBindDevice();
        }
        return _device is null ? 0 : (int)Math.Round(_device.AudioEndpointVolume.MasterVolumeLevelScalar * 100);
    }

    public bool GetMute()
    {
        if (_device is null)
        {
            TryBindDevice();
        }
        return _device?.AudioEndpointVolume.Mute ?? false;
    }

    public void SetVolume(int value)
    {
        if (_device is null)
        {
            TryBindDevice();
        }
        if (_device is null)
        {
            return;
        }
        var scalar = Math.Clamp(value, 0, 100) / 100f;
        _device.AudioEndpointVolume.MasterVolumeLevelScalar = scalar;
    }

    public bool ToggleMute()
    {
        if (_device is null)
        {
            TryBindDevice();
        }
        if (_device is null)
        {
            return false;
        }
        _device.AudioEndpointVolume.Mute = !_device.AudioEndpointVolume.Mute;
        return _device.AudioEndpointVolume.Mute;
    }

    public void Dispose()
    {
        if (_device != null)
        {
            _device.AudioEndpointVolume.OnVolumeNotification -= OnVolumeNotification;
            _device.Dispose();
        }
        _enumerator.Dispose();
    }
}
