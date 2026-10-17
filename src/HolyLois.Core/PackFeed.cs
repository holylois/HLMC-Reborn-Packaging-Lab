using System.IO.Compression;
using System.Text.Json;

namespace HolyLois.Core;

public sealed record SignedPack(byte[] Json, byte[] Signature);

public sealed class PackFeed(HttpClient client, string publicKey)
{
    public const string StableUrl = "https://github.com/holylois/HLMC-Reborn/releases/download/pack-stable/pack.json";
    // Signed asset URLs use the holylois organization from pack 2.1.0 (launcher 1.4.5+ accepts both repositories).
    public const string AssetBase = "https://github.com/holylois/HLMC-Reborn/releases/download/";

    public PackManifest Accept(SignedPack release, PackManifest current)
    {
        var next = ManifestSecurity.Parse(release.Json, release.Signature, publicKey);
        if (new Version(next.Version) < new Version(current.Version))
            throw new InvalidDataException("An older pack release was offered; your current pack was kept.");
        if (next.Defaults is null) throw new InvalidDataException("The online release has no verified defaults bundle.");
        if (next.Version == current.Version && !JsonSerializer.SerializeToUtf8Bytes(next with { Defaults = null }, JsonSettings.Options)
            .SequenceEqual(JsonSerializer.SerializeToUtf8Bytes(current with { Defaults = null }, JsonSettings.Options)))
            throw new InvalidDataException("An existing release was changed without a new version number.");
        if (next.Version == current.Version && current.Defaults is not null && next.Defaults != current.Defaults)
            throw new InvalidDataException("An existing defaults bundle was changed without a new version number.");
        return next;
    }

    public async Task<SignedPack> FetchAsync(CancellationToken token)
    {
        var pair = await SignedChannel.FetchAsync(client, StableUrl, 2 * 1024 * 1024, token);
        return new(pair.Content, pair.Signature);
    }

    public static Dictionary<string, byte[]> ReadDefaults(byte[] bytes)
    {
        if (bytes.Length > 8 * 1024 * 1024) throw new InvalidDataException("Default bundle too large.");
        var result = new Dictionary<string, byte[]>(StringComparer.OrdinalIgnoreCase);
        using var zip = new ZipArchive(new MemoryStream(bytes));
        long expanded = 0;
        if (zip.Entries.Count > 150) throw new InvalidDataException("Too many default files.");
        foreach (var entry in zip.Entries)
        {
            SafePaths.ValidateRelative(entry.FullName);
            _ = SharedDefaults.Target(entry.FullName);
            if (!entry.FullName.StartsWith("config/yosbr/", StringComparison.Ordinal) || entry.FullName.Contains("/voicechat/", StringComparison.OrdinalIgnoreCase)
                || entry.Length > 2 * 1024 * 1024 || (expanded += entry.Length) > 8 * 1024 * 1024)
                throw new InvalidDataException("Default bundle contains a personal file or exceeds its limit.");
            using var stream = entry.Open(); using var output = new MemoryStream(); stream.CopyTo(output);
            if (output.Length != entry.Length || !result.TryAdd(entry.FullName, output.ToArray())) throw new InvalidDataException("Invalid default bundle entry.");
        }
        return result;
    }
}
