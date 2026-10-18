using System.Text;
using System.Text.Json.Nodes;

namespace HolyLois.Core;

public static class SharedDefaults
{
    public static string Target(string seed)
    {
        SafePaths.ValidateRelative(seed);
        if (!seed.StartsWith("config/yosbr/", StringComparison.Ordinal)) throw new InvalidDataException("Invalid shared setting.");
        var target = seed["config/yosbr/".Length..];
        if (target != "options.txt" && !target.StartsWith("config/", StringComparison.Ordinal)) throw new InvalidDataException("Invalid shared setting target.");
        if (target.Contains("voicechat", StringComparison.OrdinalIgnoreCase) || target.Contains("account", StringComparison.OrdinalIgnoreCase)
            || target.Contains("credential", StringComparison.OrdinalIgnoreCase)) throw new InvalidDataException("Private settings cannot be shared.");
        return target;
    }

    public static byte[] Merge(string target, byte[]? personal, byte[]? previous, byte[] next)
    {
        if (personal is null) return next;
        if (target.EndsWith(".json", StringComparison.OrdinalIgnoreCase))
        {
            var current = JsonNode.Parse(personal) as JsonObject ?? throw new InvalidDataException("Cannot merge " + target);
            var newer = JsonNode.Parse(next) as JsonObject ?? throw new InvalidDataException("Invalid defaults: " + target);
            var older = previous is null ? null : JsonNode.Parse(previous) as JsonObject;
            MergeObject(current, older, newer);
            return Encoding.UTF8.GetBytes(current.ToJsonString(JsonSettings.Options));
        }
        // Xaero .cfg files are "key = value" lines too, so a shared default changes only the keys it lists.
        if (target == "options.txt" || target.EndsWith(".properties", StringComparison.OrdinalIgnoreCase)
            || target.StartsWith("config/xaero/", StringComparison.Ordinal) && target.EndsWith(".cfg", StringComparison.OrdinalIgnoreCase))
        {
            var delimiter = target == "options.txt" ? ':' : '=';
            var oldKeys = Lines(previous, delimiter); var newKeys = Lines(next, delimiter);
            var changed = newKeys.Where(x => !oldKeys.TryGetValue(x.Key, out var old) || old != x.Value)
                .Where(x => target != "options.txt" || !x.Key.StartsWith("key_", StringComparison.Ordinal) && x.Key != "lastServer")
                .ToDictionary(x => x.Key, x => x.Value);
            var lines = Encoding.UTF8.GetString(personal).Replace("\r\n", "\n").Split('\n').ToList();
            foreach (var (key, value) in changed)
            {
                var i = lines.FindIndex(line => line.StartsWith(key + delimiter, StringComparison.Ordinal));
                if (i >= 0) lines[i] = key + delimiter + value; else lines.Add(key + delimiter + value);
            }
            if (target == "options.txt") { MigrateBodyToggle(lines, oldKeys, newKeys); SeedNewKeybinds(lines, oldKeys, newKeys); ResolveKeyConflicts(lines); }
            return Encoding.UTF8.GetBytes(string.Join("\n", lines).TrimEnd('\n') + "\n");
        }
        // TOML: like options.txt, only the keys whose shared default changed are written, inside their own [section].
        if (target.EndsWith(".toml", StringComparison.OrdinalIgnoreCase) && Toml(next) is { } newToml && Toml(personal) is not null)
        {
            var oldToml = previous is null ? [] : Toml(previous) ?? [];
            var changed = newToml.Where(x => !oldToml.TryGetValue(x.Key, out var old) || old != x.Value).ToList();
            if (changed.Count == 0) return personal;
            var lines = Encoding.UTF8.GetString(personal).Replace("\r\n", "\n").Split('\n').ToList();
            foreach (var ((section, key), value) in changed) SetToml(lines, section, key, value);
            return Encoding.UTF8.GetBytes(string.Join("\n", lines).TrimEnd('\n') + "\n");
        }
        // Other formats use the reviewed complete shared file when its bytes change.
        return previous is not null && previous.SequenceEqual(next) ? personal : next;
    }

    /// <summary>"[section]" and "key = value" lines of a simple TOML file; null when it has something this merge does not
    /// understand (array tables, multi-line values), so the caller falls back to the complete file.</summary>
    private static Dictionary<(string Section, string Key), string>? Toml(byte[] bytes)
    {
        var map = new Dictionary<(string, string), string>();
        var section = "";
        foreach (var raw in Encoding.UTF8.GetString(bytes).Replace("\r\n", "\n").Split('\n'))
        {
            var line = raw.Trim();
            if (line.Length == 0 || line[0] == '#') continue;
            if (line[0] == '[') { if (!line.EndsWith(']') || line.StartsWith("[[")) return null; section = line[1..^1].Trim(); continue; }
            var eq = line.IndexOf('=');
            if (eq <= 0) return null;
            var value = line[(eq + 1)..].Trim();
            if (value.StartsWith("\"\"\"") || value.StartsWith("'''") || value.StartsWith('[') && !value.EndsWith(']')) return null;
            map[(section, line[..eq].Trim())] = value;
        }
        return map;
    }

    private static void SetToml(List<string> lines, string section, string key, string value)
    {
        var current = ""; var start = section.Length == 0 ? -1 : -2; var end = -1;
        for (var i = 0; i < lines.Count; i++)
        {
            var line = lines[i].Trim();
            if (line.StartsWith('[') && line.EndsWith(']'))
            {
                if (current == section && end < 0 && start != -2) end = i;
                current = line[1..^1].Trim();
                if (current == section) { start = i; end = -1; }
                continue;
            }
            var eq = line.IndexOf('=');
            if (current == section && eq > 0 && line[..eq].Trim() == key)
            {
                var indent = lines[i][..(lines[i].Length - lines[i].TrimStart().Length)];
                lines[i] = indent + key + " = " + value; return;
            }
        }
        if (start == -2) { lines.Add("[" + section + "]"); lines.Add(key + " = " + value); return; }
        if (end < 0) end = lines.Count;
        // A new key goes after the section's last non-empty line, with that line's indentation.
        var at = end; while (at > start + 1 && lines[at - 1].Trim().Length == 0) at--;
        var near = at > 0 ? lines[at - 1] : "";
        var pad = near.Trim().StartsWith('[') ? near[..(near.Length - near.TrimStart().Length)] + "\t" : near[..(near.Length - near.TrimStart().Length)];
        lines.Insert(at, (start < 0 ? "" : pad) + key + " = " + value);
    }

    private static void MigrateBodyToggle(List<string> lines, Dictionary<string, string> oldKeys, Dictionary<string, string> newKeys)
    {
        const string key = "key_key.firstperson.toggle", binding = "key.keyboard.page.down";
        if (!newKeys.TryGetValue(key, out var next) || next != binding
            || oldKeys.TryGetValue(key, out var previous) && previous == next) return;
        var index = lines.FindIndex(line => line.StartsWith(key + ":", StringComparison.Ordinal));
        // Only replace the former bundled F6 default or an unbound toggle, and never steal another control.
        if (index < 0 || lines[index][(key.Length + 1)..] is not ("key.keyboard.295" or "key.keyboard.f6" or "key.keyboard.unknown")) return;
        if (lines.Any(line => line.StartsWith("key_", StringComparison.Ordinal)
            && !line.StartsWith(key + ":", StringComparison.Ordinal) && line.EndsWith(":" + binding, StringComparison.Ordinal))) return;
        lines[index] = key + ":" + binding;
    }

    // A control first published in this release (normally from a newly added mod) is written once, before the
    // game would register the mod's own default. Existing lines stay personal, and a key another control already
    // uses is never taken; the mod default then applies and the player can rebind it in Controls.
    private static void SeedNewKeybinds(List<string> lines, Dictionary<string, string> oldKeys, Dictionary<string, string> newKeys)
    {
        foreach (var (key, binding) in newKeys)
        {
            if (!key.StartsWith("key_", StringComparison.Ordinal) || oldKeys.ContainsKey(key)
                || lines.Any(line => line.StartsWith(key + ":", StringComparison.Ordinal))) continue;
            if (binding != "key.keyboard.unknown" && lines.Any(line => line.StartsWith("key_", StringComparison.Ordinal)
                && line.EndsWith(":" + binding, StringComparison.Ordinal))) continue;
            lines.Add(key + ":" + binding);
        }
    }

    // Low-priority controls that give way when they share a key with another control. The first free
    // alternative is used, otherwise the control is unbound and can be set again in Controls. F3 debug
    // combinations never count as a clash because they only fire while F3 is held.
    private static readonly (string Key, string[] Alternatives)[] Yielding =
    [
        ("key_gui.xaero_open_map", ["key.keyboard.j", "key.keyboard.semicolon"]),
        ("key_key.hide_icons", []),
        ("key_key.saveToolbarActivator", []),
        ("key_key.loadToolbarActivator", []),
        ("key_zoomify.key.zoom.secondary", []),
    ];

    private static void ResolveKeyConflicts(List<string> lines)
    {
        foreach (var (key, alternatives) in Yielding)
        {
            var index = lines.FindIndex(line => line.StartsWith(key + ":", StringComparison.Ordinal));
            if (index < 0) continue;
            var binding = lines[index][(key.Length + 1)..];
            if (binding == "key.keyboard.unknown" || !KeyTaken(lines, binding, key)) continue;
            lines[index] = key + ":" + (alternatives.FirstOrDefault(free => !KeyTaken(lines, free, key)) ?? "key.keyboard.unknown");
        }
    }

    private static bool KeyTaken(List<string> lines, string binding, string except) => lines.Any(line =>
        line.StartsWith("key_", StringComparison.Ordinal) && !line.StartsWith("key_key.debug.", StringComparison.Ordinal)
        && !line.StartsWith(except + ":", StringComparison.Ordinal) && line.EndsWith(":" + binding, StringComparison.Ordinal));

    private static Dictionary<string, string> Lines(byte[]? bytes, char delimiter) => bytes is null ? [] :
        Encoding.UTF8.GetString(bytes).Replace("\r\n", "\n").Split('\n').Where(x => x.IndexOf(delimiter) > 0 && !x.StartsWith('#'))
            .GroupBy(x => x[..x.IndexOf(delimiter)]).ToDictionary(x => x.Key, x => x.Last()[(x.Last().IndexOf(delimiter) + 1)..]);
    private static void MergeObject(JsonObject target, JsonObject? old, JsonObject next)
    {
        foreach (var (key, value) in next)
        {
            if (value is JsonObject obj && target[key] is JsonObject current) MergeObject(current, old?[key] as JsonObject, obj);
            else if (old is null || !old.ContainsKey(key) || !JsonNode.DeepEquals(old[key], value)) target[key] = value?.DeepClone();
        }
    }
}
