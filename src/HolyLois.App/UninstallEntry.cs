using Microsoft.Win32;
using System.IO;

namespace HolyLois.App;

/// <summary>
/// The per-user "Installed apps" entry (HKCU, no admin rights). Windows' Uninstall button starts the installed launcher with
/// --uninstall, which shows the same remove dialog as Settings (game files and settings are kept). Only the real installation
/// registers; isolated test folders (--lab-root, --data-dir) never touch the registry.
/// </summary>
public static class UninstallEntry
{
    private const string KeyPath = @"Software\Microsoft\Windows\CurrentVersion\Uninstall\HolyLoisReborn";
    private static bool RealInstall => LauncherStartup.IsInstalled && !Environment.GetCommandLineArgs().Any(a => a is "--lab-root" or "--data-dir");

    public static void Register()
    {
        if (!RealInstall) return;
        try { Write(KeyPath, LauncherStartup.InstalledExe, LauncherStartup.InstallRoot); }
        catch (Exception ex) when (ex is UnauthorizedAccessException or IOException or System.Security.SecurityException) { LauncherErrors.Log(ex); }
    }

    private static void Write(string keyPath, string exe, string root)
    {
        using var key = Registry.CurrentUser.CreateSubKey(keyPath);
        key.SetValue("DisplayName", "Holy Lois: Reborn");
        key.SetValue("DisplayVersion", AppUpdates.RunningVersion.ToString(3));
        key.SetValue("Publisher", "Holy Lois");
        key.SetValue("DisplayIcon", exe + ",0");
        key.SetValue("InstallLocation", root);
        key.SetValue("UninstallString", "\"" + exe + "\" --uninstall");
        key.SetValue("URLInfoAbout", "https://holylois.com");
        key.SetValue("NoModify", 1, RegistryValueKind.DWord);
        key.SetValue("NoRepair", 1, RegistryValueKind.DWord);
        key.SetValue("EstimatedSize", (int)Math.Max(1, new FileInfo(exe).Length / 1024), RegistryValueKind.DWord);
    }

    /// <summary>--verify-uninstall-entry: the same write, read back and delete on a throwaway key, never the real entry.</summary>
    public static string Verify()
    {
        const string test = KeyPath + "-VerifyTest";
        var exe = Environment.ProcessPath!;
        try
        {
            Write(test, exe, Path.GetDirectoryName(exe)!);
            using (var key = Registry.CurrentUser.OpenSubKey(test) ?? throw new IOException("The test entry was not written."))
            {
                if ((string?)key.GetValue("UninstallString") != "\"" + exe + "\" --uninstall" || (string?)key.GetValue("DisplayName") != "Holy Lois: Reborn" || (int?)key.GetValue("NoModify") != 1)
                    throw new IOException("The test entry has wrong values.");
            }
        }
        finally { Registry.CurrentUser.DeleteSubKeyTree(test, false); }
        if (Registry.CurrentUser.OpenSubKey(test) is not null) throw new IOException("The test entry was not removed.");
        return "Uninstall entry written, read back and removed on " + test + ".";
    }

    public static void Remove()
    {
        try { Registry.CurrentUser.DeleteSubKeyTree(KeyPath, false); }
        catch (Exception ex) when (ex is UnauthorizedAccessException or IOException or System.Security.SecurityException) { LauncherErrors.Log(ex); }
    }
}
