using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;

namespace RemoteHost.Services;

public static class SubnetGuard
{
    public static bool IsAllowed(IPAddress remote)
    {
        if (IPAddress.IsLoopback(remote))
        {
            return true;
        }

        if (remote.IsIPv4MappedToIPv6)
        {
            remote = remote.MapToIPv4();
        }

        if (remote.AddressFamily != AddressFamily.InterNetwork)
        {
            return false;
        }

        foreach (var (localIp, mask) in GetActiveIPv4Interfaces())
        {
            if (IsSameSubnet(remote, localIp, mask))
            {
                return true;
            }
        }

        return false;
    }

    public static IEnumerable<(IPAddress Address, IPAddress Mask)> GetActiveIPv4Interfaces()
    {
        foreach (var nic in NetworkInterface.GetAllNetworkInterfaces())
        {
            if (nic.OperationalStatus != OperationalStatus.Up)
            {
                continue;
            }
            if (nic.NetworkInterfaceType == NetworkInterfaceType.Loopback)
            {
                continue;
            }

            IPInterfaceProperties props;
            try
            {
                props = nic.GetIPProperties();
            }
            catch
            {
                continue;
            }

            foreach (var ua in props.UnicastAddresses)
            {
                if (ua.Address.AddressFamily != AddressFamily.InterNetwork)
                {
                    continue;
                }
                if (ua.IPv4Mask == null)
                {
                    continue;
                }
                yield return (ua.Address, ua.IPv4Mask);
            }
        }
    }

    public static IPAddress? GetPrimaryIPv4()
    {
        return GetActiveIPv4Interfaces()
            .Select(t => t.Address)
            .FirstOrDefault(a => !a.ToString().StartsWith("169.254", StringComparison.Ordinal));
    }

    private static bool IsSameSubnet(IPAddress a, IPAddress b, IPAddress mask)
    {
        var aBytes = a.GetAddressBytes();
        var bBytes = b.GetAddressBytes();
        var mBytes = mask.GetAddressBytes();
        if (aBytes.Length != bBytes.Length || aBytes.Length != mBytes.Length)
        {
            return false;
        }

        for (var i = 0; i < aBytes.Length; i++)
        {
            if ((aBytes[i] & mBytes[i]) != (bBytes[i] & mBytes[i]))
            {
                return false;
            }
        }

        return true;
    }
}
