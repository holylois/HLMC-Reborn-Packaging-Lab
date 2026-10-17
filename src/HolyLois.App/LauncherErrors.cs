using HolyLois.Core;
using System.IO;
using System.Windows;

namespace HolyLois.App;

/// <summary>Unexpected errors after startup: written to logs/launcher-error.txt (included in reports) and explained once.</summary>
public static class LauncherErrors
{
    public static string LogPath => Path.Combine(LauncherStartup.InstallRoot, "data", "logs", "launcher-error.txt");
    private static bool showing;

    public static void Log(Exception exception)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(LogPath)!);
            if (File.Exists(LogPath) && new FileInfo(LogPath).Length > 256 * 1024) File.Move(LogPath, LogPath + ".old", true);
            File.AppendAllText(LogPath, DateTimeOffset.Now.ToString("yyyy-MM-dd HH:mm:ss") + " app " + AppUpdates.RunningVersion.ToString(3) + Environment.NewLine + exception + Environment.NewLine + Environment.NewLine);
        }
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException) { }
    }

    public static void Report(Exception exception)
    {
        Log(exception);
        if (showing) return;
        showing = true;
        try { AppDialog.Show(Application.Current?.MainWindow, Localize.Text("UnexpectedTitle"), Localize.Text("UnexpectedInfo") + "\n\n" + Localize.Message(exception)); }
        catch { /* the error is logged; a broken dialog must not end the app */ }
        finally { showing = false; }
    }
}
