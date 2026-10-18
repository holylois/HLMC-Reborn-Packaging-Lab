using System.Text.Json;

namespace HolyLois.Core;

/// <summary>Authored translations change presentation only. Signed versions, dates and installation data stay authoritative.</summary>
public sealed class PlayerNotes
{
    private readonly Dictionary<string, JsonElement> releases = new(StringComparer.Ordinal);
    private readonly List<JsonElement> launcher = [];
    public PlayerNotes(byte[] bytes)
    {
        try
        {
            if (bytes.Length > 512 * 1024) return;
            using var doc = JsonDocument.Parse(bytes);
            foreach (var entry in doc.RootElement.GetProperty("releases").EnumerateObject()) releases[entry.Name] = entry.Value.Clone();
            if (doc.RootElement.TryGetProperty("launcher", out var apps))
                foreach (var entry in apps.EnumerateObject()) if (entry.Value.TryGetProperty("version", out var v) && v.GetString() == entry.Name) launcher.Add(entry.Value.Clone());
        }
        catch (JsonException) { }
        catch (InvalidOperationException) { }
        catch (KeyNotFoundException) { }
    }
    /// <summary>The launcher's own release notes (newest first), shipped inside the EXE, so "What changed" can show each launcher
    /// release as its own entry next to the pack's. Entries with a bad shape are skipped.</summary>
    public IReadOnlyList<ReleaseNote> Launcher(string language)
    {
        var list = new List<ReleaseNote>();
        foreach (var entry in launcher)
        {
            try
            {
                var version = entry.GetProperty("version").GetString() ?? "";
                var date = entry.GetProperty("date").GetString() ?? "";
                if (version.Length is < 1 or > 20 || date.Length != 10) continue;
                var shell = new ReleaseNote(version, date, "", [], [], []);
                var note = Localized(entry, shell, language);
                if (!ReferenceEquals(note, shell)) list.Add(note);
            }
            catch (InvalidOperationException) { }
            catch (KeyNotFoundException) { }
        }
        return list;
    }
    public ReleaseNote For(ReleaseNote signed, string language)
    {
        try
        {
            if (!releases.TryGetValue(signed.Version, out var entry) || entry.GetProperty("version").GetString() != signed.Version) return signed;
            return Localized(entry, signed, language);
        }
        catch (JsonException) { return signed; }
        catch (InvalidOperationException) { return signed; }
        catch (KeyNotFoundException) { return signed; }
    }
    private static ReleaseNote Localized(JsonElement entry, ReleaseNote signed, string language)
    {
        try
        {
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
