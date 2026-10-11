using System.Text.Json;

namespace HolyLois.Core;

/// <summary>Authored translations change presentation only. Signed versions, dates and installation data stay authoritative.</summary>
public sealed class PlayerNotes
{
    private readonly Dictionary<string, JsonElement> releases = new(StringComparer.Ordinal);
    public PlayerNotes(byte[] bytes)
    {
        try
        {
            if (bytes.Length > 512 * 1024) return;
            using var doc = JsonDocument.Parse(bytes);
            foreach (var entry in doc.RootElement.GetProperty("releases").EnumerateObject()) releases[entry.Name] = entry.Value.Clone();
        }
        catch (JsonException) { }
        catch (InvalidOperationException) { }
        catch (KeyNotFoundException) { }
    }
    public ReleaseNote For(ReleaseNote signed, string language)
    {
        try
        {
            if (!releases.TryGetValue(signed.Version, out var entry) || entry.GetProperty("version").GetString() != signed.Version) return signed;
            var locales = entry.GetProperty("locales");
            if (!locales.TryGetProperty(language, out var text) && !locales.TryGetProperty("en", out text)) return signed;
            var summary = text.GetProperty("summary").GetString();
            if (string.IsNullOrWhiteSpace(summary) || summary.Length > 2000) return signed;
            string[] Read(string key) => text.TryGetProperty(key, out var value) ? value.EnumerateArray().Select(x => x.GetString() ?? "").ToArray() : [];
            var flat = text.TryGetProperty("changes", out _);
            var added = flat ? Array.Empty<string>() : Read("added");
            var updated = Read(flat ? "changes" : "changed");
            var fixedLines = flat ? Array.Empty<string>() : Read("fixed");
            var removed = flat ? Array.Empty<string>() : Read("removed");
            var all = added.Concat(updated).Concat(fixedLines).Concat(removed).ToArray();
            if (all.Length is < 1 or > 40 || all.Any(line => string.IsNullOrWhiteSpace(line) || line.Length > 2000)) return signed;
            return signed with { Summary = summary, Added = added, Updated = updated, Fixed = fixedLines, Removed = removed };
        }
        catch (JsonException) { return signed; }
        catch (InvalidOperationException) { return signed; }
        catch (KeyNotFoundException) { return signed; }
    }
}
