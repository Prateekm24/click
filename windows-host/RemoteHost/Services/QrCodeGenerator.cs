using System.IO;
using System.Text.Json;
using System.Windows.Media.Imaging;
using QRCoder;
using RemoteHost.Models;

namespace RemoteHost.Services;

public static class QrCodeGenerator
{
    public static BitmapImage GeneratePairingQr(PairingPayload payload)
    {
        var json = JsonSerializer.Serialize(payload, ProtocolJson.Options);

        // QRCodeGenerator/QRCodeData are left undisposed here rather than wrapped in
        // "using" — they hold no unmanaged resources, just bit arrays, and ordinary GC
        // cleanup is sufficient.
        var qrGenerator = new QRCodeGenerator();
        var qrData = qrGenerator.CreateQrCode(json, QRCodeGenerator.ECCLevel.M);
        var pngQr = new PngByteQRCode(qrData);
        var bytes = pngQr.GetGraphic(10);

        return BytesToBitmapImage(bytes);
    }

    private static BitmapImage BytesToBitmapImage(byte[] bytes)
    {
        using var ms = new MemoryStream(bytes);
        var image = new BitmapImage();
        image.BeginInit();
        image.CacheOption = BitmapCacheOption.OnLoad;
        image.StreamSource = ms;
        image.EndInit();
        image.Freeze();
        return image;
    }
}
