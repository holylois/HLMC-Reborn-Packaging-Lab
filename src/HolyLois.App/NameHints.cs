using HolyLois.Core;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Threading;

namespace HolyLois.App;

/// <summary>
/// Live name check under a name box: green when the name is free and will be locked to the player, orange when it belongs to
/// a bought Minecraft account, red when somebody already plays with it here. It only informs; saving is never blocked.
/// </summary>
public static class NameHints
{
    public static void Attach(TextBox box, TextBlock hint, ClientContext context)
    {
        CancellationTokenSource? running = null;
        var timer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(450) };
        // Clear the text but keep the line, so the window does not jump on every key.
        box.TextChanged += (_, _) => { running?.Cancel(); hint.Text = ""; timer.Stop(); timer.Start(); };
        timer.Tick += async (_, _) =>
        {
            timer.Stop();
            var name = box.Text.Trim();
            if (name.Length == 0) return;
            running?.Cancel();
            var mine = running = new CancellationTokenSource(TimeSpan.FromSeconds(8));
            Show(hint, "NameChecking", "Muted");
            NameStatus status;
            try { status = await context.CheckNameAsync(name, mine.Token); }
            catch (OperationCanceledException) { return; }
            if (mine.IsCancellationRequested || box.Text.Trim() != name) return;
            switch (status)
            {
                case NameStatus.Invalid: Show(hint, "NameInvalid", "Danger"); break;
                case NameStatus.Yours: Show(hint, "NameYours", "Muted"); break;
                case NameStatus.Taken: Show(hint, "NameTaken", "Danger", "✕ "); break;
                case NameStatus.Premium: Show(hint, "NamePremium", "Warning", "! "); break;
                case NameStatus.Free: Show(hint, "NameFree", "Success", "✓ "); break;
                default: Show(hint, "NameCheckFailed", "Muted"); break;
            }
        };
    }

    /// <summary>
    /// Before a new name is saved: a bought account's name needs the player's explicit OK, because its owner can claim it
    /// later (owner decision 2026-10-18: the free-play account is then moved to backup). Unknown or offline counts as OK.
    /// </summary>
    public static async Task<bool> ConfirmAsync(Window owner, ClientContext context, string name)
    {
        NameStatus status;
        try { using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(8)); status = await context.CheckNameAsync(name, timeout.Token); }
        catch (OperationCanceledException) { return true; }
        return status != NameStatus.Premium || AppDialog.Show(owner, Localize.Text("PremiumConfirmTitle"), string.Format(Localize.Text("PremiumConfirmInfo"), name), Localize.Text("PremiumConfirmAgree"));
    }

    private static void Show(TextBlock hint, string key, string brush, string mark = "")
    {
        hint.Text = mark + Localize.Text(key);
        hint.Foreground = (Brush)Application.Current.Resources[brush];
        hint.Visibility = Visibility.Visible;
    }
}
