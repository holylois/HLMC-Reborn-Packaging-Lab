using System.Diagnostics;
using System.IO;
using System.Text;
using System.Text.Json;

namespace HolyLois.App;

public static class LauncherDiscovery
{
    private static readonly Lazy<Task<Dictionary<string, string>>> registered = new(() => Task.Run(ReadStartApps));
    public static bool Ready => registered.Value.IsCompleted;
    public static async Task WarmAsync() => await registered.Value;
    public static string? Registered(string launcher) => registered.Value.IsCompletedSuccessfully
        ? registered.Value.Result.GetValueOrDefault(launcher) : null;

    // Fast path without starting PowerShell: the Store launcher's package in the registry and SKlauncher's Start menu
    // shortcut. Get-StartApps stays as the fallback when neither is found.
    private static Dictionary<string, string> ReadStartApps()
    {
        var result = new Dictionary<string, string>();
        try
        {
            using var packages = Microsoft.Win32.Registry.CurrentUser.OpenSubKey(@"Software\Classes\Local Settings\Software\Microsoft\Windows\CurrentVersion\AppModel\Repository\Packages");
            if (packages?.GetSubKeyNames().Any(n => n.StartsWith("Microsoft.4297127D64EC6_", StringComparison.OrdinalIgnoreCase) && n.EndsWith("_8wekyb3d8bbwe", StringComparison.OrdinalIgnoreCase)) == true)
                result["official"] = @"shell:AppsFolder\Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft";
            var menus = new[] { Environment.GetFolderPath(Environment.SpecialFolder.Programs), Environment.GetFolderPath(Environment.SpecialFolder.CommonPrograms) };
            var options = new EnumerationOptions { RecurseSubdirectories = true, IgnoreInaccessible = true, MaxRecursionDepth = 3 };
            var sk = menus.Where(Directory.Exists).SelectMany(m => Directory.EnumerateFiles(m, "SKlauncher*.lnk", options)).OrderBy(p => p.Length).FirstOrDefault();
            if (sk is not null) result["sk"] = sk;
        }
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException or System.Security.SecurityException) { }
        return result.Count > 0 ? result : ReadWithPowerShell();
    }

    private static Dictionary<string, string> ReadWithPowerShell()
    {
        try
        {
            var start = new ProcessStartInfo(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System), "WindowsPowerShell/v1.0/powershell.exe"))
            { UseShellExecute = false, CreateNoWindow = true, RedirectStandardOutput = true, RedirectStandardError = true, StandardOutputEncoding = Encoding.UTF8 };
            foreach (var argument in new[] { "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", "[Console]::OutputEncoding=[Text.UTF8Encoding]::new(); @(Get-StartApps | Where-Object { $_.Name -eq 'Minecraft Launcher' -or $_.Name -match '^SKlauncher( |$)' } | Select-Object Name,AppID) | ConvertTo-Json -Compress" }) start.ArgumentList.Add(argument);
            using var process = Process.Start(start)!;
            var outputTask = process.StandardOutput.ReadToEndAsync();
            var errorTask = process.StandardError.ReadToEndAsync();
            if (!process.WaitForExit(7000)) { process.Kill(); return []; }
            var output = outputTask.GetAwaiter().GetResult();
            if (process.ExitCode != 0 || output.Length > 65536 || string.IsNullOrWhiteSpace(output)) return [];
            using var json = JsonDocument.Parse(output);
            var result = new Dictionary<string, string>();
            foreach (var entry in json.RootElement.EnumerateArray())
            {
                var name = entry.GetProperty("Name").GetString()!; var id = entry.GetProperty("AppID").GetString()!;
                if (id.Any(char.IsControl) || id.Length > 512) continue;
                var key = name == "Minecraft Launcher" ? "official" : "sk";
                result[key] = File.Exists(id) ? id : "shell:AppsFolder\\" + id;
            }
            return result;
        }
        catch { return []; }
    }
}
