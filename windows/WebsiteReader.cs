using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Sockets;
using System.Text;
using System.Text.RegularExpressions;

namespace DimaAI.Windows;

public static class WebsiteReader
{
    private const int MaxPageBytes = 1_500_000;
    private const int MaxPageChars = 8_000;
    private static readonly Regex UrlPattern = new("https://[^\\s<>\"']+", RegexOptions.IgnoreCase);

    public static string? FindUrl(string text) => UrlPattern.Match(text).Value.TrimEnd('.', ',', '!', '?', ')', ']', '}') is { Length: > 0 } value
        ? value
        : null;

    public static async Task<(string Url, string Text)?> ReadAsync(string address, CancellationToken cancellationToken)
    {
        using var handler = new HttpClientHandler { AllowAutoRedirect = false, AutomaticDecompression = DecompressionMethods.All };
        using var client = new HttpClient(handler) { Timeout = TimeSpan.FromSeconds(20) };
        var current = new Uri(address);

        for (var redirects = 0; redirects <= 4; redirects++)
        {
            await ValidatePublicAddressAsync(current, cancellationToken);
            using var response = await client.GetAsync(current, HttpCompletionOption.ResponseHeadersRead, cancellationToken);
            if ((int)response.StatusCode is 301 or 302 or 303 or 307 or 308)
            {
                if (response.Headers.Location is null)
                    return null;
                current = response.Headers.Location.IsAbsoluteUri
                    ? response.Headers.Location
                    : new Uri(current, response.Headers.Location);
                continue;
            }
            response.EnsureSuccessStatusCode();

            var mediaType = response.Content.Headers.ContentType?.MediaType ?? "";
            if (mediaType is not ("text/html" or "text/plain" or "application/xhtml+xml"))
                throw new InvalidDataException("Поддерживаются HTML и обычный текст, но не PDF.");
            if (response.Content.Headers.ContentLength > MaxPageBytes)
                throw new InvalidDataException("Страница слишком большая для чтения.");

            await using var stream = await response.Content.ReadAsStreamAsync(cancellationToken);
            using var output = new MemoryStream();
            var buffer = new byte[16 * 1024];
            while (true)
            {
                var count = await stream.ReadAsync(buffer, cancellationToken);
                if (count == 0)
                    break;
                if (output.Length + count > MaxPageBytes)
                    throw new InvalidDataException("Страница слишком большая для чтения.");
                await output.WriteAsync(buffer.AsMemory(0, count), cancellationToken);
            }

            var html = Encoding.UTF8.GetString(output.ToArray());
            html = Regex.Replace(html, "(?is)<(script|style|noscript|svg|iframe)[^>]*>.*?</\\1>", " ");
            var text = WebUtility.HtmlDecode(Regex.Replace(html, "(?s)<[^>]+>", " "));
            text = Regex.Replace(text, @"\s+", " ").Trim();
            if (text.Length > MaxPageChars)
                text = text[..MaxPageChars];
            return text.Length == 0 ? null : (current.ToString(), text);
        }
        return null;
    }

    private static async Task ValidatePublicAddressAsync(Uri uri, CancellationToken cancellationToken)
    {
        if (uri.Scheme != Uri.UriSchemeHttps || string.IsNullOrWhiteSpace(uri.Host))
            throw new InvalidDataException("Разрешены только публичные HTTPS-ссылки.");

        var addresses = await Dns.GetHostAddressesAsync(uri.DnsSafeHost, cancellationToken);
        if (addresses.Length == 0 || addresses.Any(IsPrivateAddress))
            throw new InvalidDataException("Локальные и приватные адреса запрещены.");
    }

    private static bool IsPrivateAddress(IPAddress address)
    {
        if (IPAddress.IsLoopback(address) || address.IsIPv6LinkLocal || address.IsIPv6SiteLocal ||
            address.IsIPv6Multicast || address.IsIPv4MappedToIPv6)
            return true;
        if (address.AddressFamily == AddressFamily.InterNetworkV6)
            return (address.GetAddressBytes()[0] & 0xFE) == 0xFC;
        var bytes = address.GetAddressBytes();
        return bytes[0] == 10 || bytes[0] == 127 ||
               (bytes[0] == 172 && bytes[1] is >= 16 and <= 31) ||
               (bytes[0] == 192 && bytes[1] == 168) ||
               (bytes[0] == 169 && bytes[1] == 254);
    }
}