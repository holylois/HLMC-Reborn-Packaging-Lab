package holylois.boombox;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * /cinematic on|off and /cinematic fishing on|off (owner 2026-10-11): switch Ji AFK Cinematic's idle camera and its fishing camera
 * without digging through its menu. Saved in its own config, so the choice stays after a restart. Turning it off mid-shot ends it.
 */
public final class CinematicToggle {
    private CinematicToggle() {}

    static void register() {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ji-afk-cinematic")) return;
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(ClientCommands.literal("cinematic")
            .executes(c -> status())
            .then(ClientCommands.literal("on").executes(c -> set("modEnabled", true)))
            .then(ClientCommands.literal("off").executes(c -> set("modEnabled", false)))
            .then(ClientCommands.literal("fishing")
                .executes(c -> status())
                .then(ClientCommands.literal("on").executes(c -> set("fishingCinematicEnabled", true)))
                .then(ClientCommands.literal("off").executes(c -> set("fishingCinematicEnabled", false))))));
    }

    private static Object config() throws ReflectiveOperationException {
        return Class.forName("com.ji.afkcinematic.config.ConfigManager").getMethod("getConfig").invoke(null);
    }

    /** Current switches, or null when Ji's config cannot be read. */
    public static boolean[] state() {
        try { var c = config(); return new boolean[]{c.getClass().getField("modEnabled").getBoolean(c), c.getClass().getField("fishingCinematicEnabled").getBoolean(c)}; }
        catch (ReflectiveOperationException | RuntimeException | LinkageError e) { return null; }
    }

    public static int set(String field, boolean on) {
        try {
            var c = config();
            c.getClass().getField(field).setBoolean(c, on);
            Class.forName("com.ji.afkcinematic.config.ConfigManager").getMethod("saveConfig").invoke(null);
            if (!on && (field.equals("modEnabled") || CinematicTweaks.fishing) && CinematicTweaks.playing())
                Class.forName("com.ji.afkcinematic.cinematic.CinematicManager").getMethod("forceDeactivate").invoke(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            say(Component.translatable("holylois.cinematic.unavailable"));
            return 0;
        }
        return status();
    }

    private static int status() {
        var s = state();
        if (s == null) { say(Component.translatable("holylois.cinematic.unavailable")); return 0; }
        say(Component.translatable("holylois.cinematic.status", word(s[0]), word(s[1])));
        return 1;
    }

    private static Component word(boolean on) {
        return Component.translatable(on ? "holylois.cinematic.on" : "holylois.cinematic.off").withStyle(s -> s.withColor((on ? Ui.GREEN : Ui.MUTED) & 0xFFFFFF));
    }

    private static void say(Component text) { var p = Minecraft.getInstance().player; if (p != null) p.sendSystemMessage(text); }
}
