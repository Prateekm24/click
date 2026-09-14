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

    // A plain "first active adapter" pick is unreliable on a real Windows machine: Hyper-V
    // (Docker Desktop, WSL2), VMware/VirtualBox, and VPN clients all install virtual
    // adapters that report OperationalStatus.Up with a normal-looking private IPv4 address,
    // and .NET does not enumerate them in any particular order relative to the real
    // Wi-Fi/Ethernet adapter. Advertising one of those in the pairing QR code produces an
    // address the phone can never reach even though both devices are "on the same Wi-Fi".
    // A LAN-facing adapter handed an address by a real router has a default gateway;
    // internal-only virtual switches do not — so prefer whichever active adapter has one.
    public static IPAddress? GetPrimaryIPv4()
    {
        var candidates = GetActiveIPv4InterfacesWithGateway()
            .Where(t => !t.Address.ToString().StartsWith("169.254", StringComparison.Ordinal))
            .ToList();

        var withGateway = candidates.FirstOrDefault(t => t.HasGateway);
        if (withGateway.Address is not null)
        {
            return withGateway.Address;
        }

        return candidates.FirstOrDefault().Address;
    }

    private static IEnumerable<(IPAddress Address, IPAddress Mask, bool HasGateway)> GetActiveIPv4InterfacesWithGateway()
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

            var hasGateway = props.GatewayAddresses.Any(g =>
                g.Address.AddressFamily == AddressFamily.InterNetwork && !g.Address.Equals(IPAddress.Any));

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
                yield return (ua.Address, ua.IPv4Mask, hasGateway);
            }
        }
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
