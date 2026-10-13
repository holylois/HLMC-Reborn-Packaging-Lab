using System.Net.Http.Headers;

namespace HolyLois.Core;

/// <summary>Mutable signed channels need a fresh request identity; immutable payload downloads still use their cache.</summary>
internal static class SignedChannel
{
    internal static async Task<(byte[] Content, byte[] Signature)> FetchAsync(HttpClient client, string url, int limit, CancellationToken token)
    {
        var requestId = Guid.NewGuid().ToString("N");
        var content = await ReadAsync(client, url, requestId, limit, token);
        var signature = await ReadAsync(client, url + ".sig", requestId, 512, token);
        return (content, signature);
    }

    private static async Task<byte[]> ReadAsync(HttpClient client, string url, string requestId, int limit, CancellationToken token)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, url + "?hl=" + requestId);
        request.Headers.CacheControl = new CacheControlHeaderValue { NoCache = true, NoStore = true };
        using var response = await client.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, token);
        response.EnsureSuccessStatusCode();
        ManifestSecurity.ValidateDownloadUri(response.RequestMessage?.RequestUri?.AbsoluteUri ?? request.RequestUri!.AbsoluteUri);
        if (response.Content.Headers.ContentLength > limit) throw new InvalidDataException("Signed metadata exceeds its size limit.");
        using var output = new MemoryStream();
        await using var input = await response.Content.ReadAsStreamAsync(token);
        var buffer = new byte[8192]; int count;
        while ((count = await input.ReadAsync(buffer, token)) > 0)
        {
            if (output.Length + count > limit) throw new InvalidDataException("Signed metadata exceeds its size limit.");
            output.Write(buffer, 0, count);
        }
        return output.ToArray();
    }
}
