using System.Globalization;
using System.Text.Json;
using System.Windows;
using System.Windows.Markup;
namespace HolyLois.App;
public static class Localize
{
    private static readonly Dictionary<string, Dictionary<string, string>> all = JsonSerializer.Deserialize<Dictionary<string, Dictionary<string, string>>>(ClientContext.Asset("strings.json"))!;
    private static readonly HolyLois.Core.PlayerNotes playerNotes = new(ClientContext.Asset("player-notes.json"));
    public static HolyLois.Core.ReleaseNote Note(HolyLois.Core.ReleaseNote signed) => playerNotes.For(signed, Language);
    public static IReadOnlyList<HolyLois.Core.ReleaseNote> LauncherNotes() => playerNotes.Launcher(Language);
    public static string Language { get; private set; } = "en";
    public static string Text(string key) => all[Language].GetValueOrDefault(key) ?? all["en"].GetValueOrDefault(key) ?? key;
    /// <summary>Picks the language before any window exists (the update check runs first).</summary>
    public static void Use(string language)
    {
        Language = all.ContainsKey(language) ? language : "en";
        var culture = CultureInfo.GetCultureInfo(Language);
        CultureInfo.CurrentUICulture = culture; CultureInfo.CurrentCulture = culture;
        foreach (var (key, value) in all["en"]) Application.Current.Resources["L." + key] = all[Language].GetValueOrDefault(key) ?? value;
    }
    public static void Apply(Window window, string language)
    {
        Use(language);
        window.Language = XmlLanguage.GetLanguage(CultureInfo.GetCultureInfo(Language).IetfLanguageTag);
    }
    // Core and app messages are written in English; the ones players meet get their RU/LV text here.
    private static readonly (string Start, string Key)[] Known =
    [
        ("Close Minecraft and your Minecraft launcher", "CloseGame"),
        ("You added ", "ErrNamesToday"),
        ("Use 3 to 16 letters", "NameInvalid"),
        ("Choose a player name first", "NameInvalid"),
        ("Close SKlauncher", "ErrCloseSk"),
        ("Click Check files", "ErrCheckFiles"),
        ("Install Holy Lois before starting it", "ErrCheckFiles"),
        ("Choose your player name first", "ErrChooseName"),
        ("Close Minecraft before changing shader settings", "ErrCloseShaders"),
        ("Wait for the release check", "ErrWaitCheck"),
        ("Choose the game folder of the imported", "ErrSkFolder"),
        ("Choose your installed launcher", "ErrLocate"),
    ];
    /// <summary>The player-facing text of an error, without the "Error" prefix.</summary>
    public static string Message(Exception exception) => exception switch
    {
        NotEnoughSpaceException space => string.Format(Text("ErrNoSpace"), space.Drive, space.Needed / 1073741824.0, space.Free / 1073741824.0),
        HolyLois.Core.DownloadFailedException => Text("ErrDownload"),
        _ => Known.FirstOrDefault(k => exception.Message.StartsWith(k.Start, StringComparison.Ordinal)).Key is { } key ? Text(key) : exception.Message,
    };
    public static string Error(Exception exception) => exception.Message.StartsWith("Close Minecraft and your", StringComparison.Ordinal)
        ? Text("CloseGame") : exception is NotEnoughSpaceException or HolyLois.Core.DownloadFailedException ? Message(exception) : Text("Error") + " " + Message(exception);
}
