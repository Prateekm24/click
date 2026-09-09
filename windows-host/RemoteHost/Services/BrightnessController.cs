using System.Linq;
using System.Management;

namespace RemoteHost.Services;

// Backed by the root\WMI WmiMonitorBrightness(Methods) classes, which only exist when the
// active display's driver exposes DDC/CI-less brightness control (built-in laptop panels,
// typically). External monitors and many docked setups will not expose this WMI namespace
// at all, so every call here must fail soft instead of throwing.
public sealed class BrightnessController
{
    public int? GetBrightness()
    {
        try
        {
            using var searcher = new ManagementObjectSearcher("root\\WMI", "SELECT * FROM WmiMonitorBrightness");
            foreach (ManagementBaseObject mo in searcher.Get())
            {
                using (mo)
                {
                    return Convert.ToInt32(mo["CurrentBrightness"]);
                }
            }
        }
        catch
        {
            // Not supported on this display.
        }
        return null;
    }

    public bool SetBrightness(int value)
    {
        value = Math.Clamp(value, 0, 100);
        try
        {
            using var searcher = new ManagementObjectSearcher("root\\WMI", "SELECT * FROM WmiMonitorBrightnessMethods");
            foreach (ManagementObject mo in searcher.Get().Cast<ManagementObject>())
            {
                using (mo)
                {
                    mo.InvokeMethod("WmiSetBrightness", new object[] { (uint)1, (byte)value });
                    return true;
                }
            }
        }
        catch
        {
            // Not supported on this display.
        }
        return false;
    }
}
