using System.Text.Json;

namespace HolyLois.Core;

public sealed record SetupChoices(string Launcher, bool DesktopShortcut, bool StartMenuShortcut);

public static class LauncherSetup
{
    // The old marker is a migration boundary, including shortcuts the player deleted. After the app was removed (it leaves
    // removal-result.txt and keeps the player's settings) the next start is a reinstall and offers setup and shortcuts again.
    public static bool NeedsSetup(string baseFolder, bool existingPlayerSettings) =>
        !File.Exists(SafePaths.Resolve(baseFolder, "setup-completed.json")) &&
        (File.Exists(SafePaths.Resolve(baseFolder, RemovedMarker)) ||
         !File.Exists(SafePaths.Resolve(baseFolder, "shortcuts-initialized.txt")) && !existingPlayerSettings);
    public const string RemovedMarker = "removal-result.txt";

    public static void Complete(string baseFolder, SetupChoices choices, Action<bool> createShortcut)
    {
        if (choices.Launcher is not ("official" or "sk" or "name")) throw new InvalidDataException("Choose a Minecraft account or a player name.");
        var receipt = SafePaths.Resolve(baseFolder, "setup-completed.json");
        if (File.Exists(receipt)) return;
        if (choices.DesktopShortcut) createShortcut(true);
        if (choices.StartMenuShortcut) createShortcut(false);
        AtomicFiles.WriteJson(receipt, choices);
        AtomicFiles.Write(SafePaths.Resolve(baseFolder, "shortcuts-initialized.txt"), "User selected shortcuts during setup."u8.ToArray());
        var removed = SafePaths.Resolve(baseFolder, RemovedMarker);
        if (File.Exists(removed)) File.Delete(removed);
    }

    public static void Migrate(string baseFolder, string launcher)
    {
        var receipt = SafePaths.Resolve(baseFolder, "setup-completed.json");
        if (!File.Exists(receipt)) AtomicFiles.WriteJson(receipt, new SetupChoices(launcher, false, false));
    }
}
