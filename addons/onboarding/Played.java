package holylois;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.stats.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /played (owner 2026-10-17): your total, active and AFK time, plus fun totals. Fishing time (a cast line) and boombox time
 * (hearing a playing boombox) are counted once per non-AFK second into world/holylois/fun-time.json from this release on;
 * the rest comes from vanilla stats. /played NAME is for operators, like /seen (no stalking), and works for online players.
 */
final class Played {
    private static final Logger LOG = LoggerFactory.getLogger("HolyLois");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    static final class State { public int version = 1; public Map<String, Long> fishing = new HashMap<>(), music = new HashMap<>(); }

    private static State state = new State();
    private static Path file;
    private static boolean dirty;

    private Played() {}

    static void load(Path worldRoot) {
        file = worldRoot.resolve("holylois/fun-time.json");
        try {
            if (Files.exists(file)) {
                var loaded = JSON.fromJson(Files.readString(file), State.class);
                if (loaded != null && loaded.fishing != null && loaded.music != null) state = loaded;
            }
        } catch (Exception error) { LOG.warn("Cannot read the fun-time ledger; starting empty in memory", error); }
    }

    /** Once per second. AFK seconds never count as fun. */
    static void tick(MinecraftServer server) {
        if (file == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (Afk.afkNow(player)) continue;
            String id = player.getUUID().toString();
            if (player.fishing != null) { state.fishing.merge(id, 1L, Long::sum); dirty = true; }
            if (listening(player)) { state.music.merge(id, 1L, Long::sum); dirty = true; }
        }
        if (dirty && server.getTickCount() % 1200 == 0) save();
    }

    private static boolean listening(ServerPlayer player) {
        try { return holylois.boombox.Boombox.hearing(player.getUUID()); }
        catch (LinkageError error) { return false; }
    }

    static synchronized long fishing(UUID id) { return state.fishing.getOrDefault(id.toString(), 0L); }
    static synchronized long music(UUID id) { return state.music.getOrDefault(id.toString(), 0L); }

    static synchronized void save() {
        if (file == null || !dirty) return;
        try {
            Files.createDirectories(file.getParent());
            var temporary = file.resolveSibling("fun-time.json.tmp");
            Files.writeString(temporary, JSON.toJson(state));
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            dirty = false;
        } catch (Exception error) { LOG.warn("Cannot save the fun-time ledger", error); }
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("played")
            .executes(context -> show(context.getSource(), context.getSource().getPlayerOrException(), true))
            .then(Commands.argument("player", StringArgumentType.word())
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> {
                    var source = context.getSource();
                    String name = StringArgumentType.getString(context, "player");
                    var target = source.getServer().getPlayerList().getPlayerByName(name);
                    if (target == null) { source.sendFailure(Component.literal(name + " is not online. /played NAME works for online players.")); return 0; }
                    return show(source, target, false);
                })));
    }

    private static int show(CommandSourceStack source, ServerPlayer player, boolean self) {
        var stats = player.getStats();
        long playTicks = stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        long afk = Afk.seconds(player.getUUID());
        long walkedCm = stats.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)) + stats.getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM))
            + stats.getValue(Stats.CUSTOM.get(Stats.CROUCH_ONE_CM));
        for (String line : lines(self ? "Your" : player.getGameProfile().name() + "'s", playTicks, afk, fishing(player.getUUID()), music(player.getUUID()),
                stats.getValue(Stats.CUSTOM.get(Stats.FISH_CAUGHT)), walkedCm, stats.getValue(Stats.CUSTOM.get(Stats.BOAT_ONE_CM)),
                stats.getValue(Stats.CUSTOM.get(Stats.MOB_KILLS)), stats.getValue(Stats.CUSTOM.get(Stats.DEATHS))))
            source.sendSystemMessage(Component.literal(line).withStyle(line.startsWith("  ") ? ChatFormatting.GRAY : ChatFormatting.GOLD));
        return 1;
    }

    /** The chat lines. Pure, for tests. */
    static List<String> lines(String whose, long playTicks, long afkSeconds, long fishingSeconds, long musicSeconds,
                              long fish, long walkedCm, long boatCm, long kills, long deaths) {
        long total = playTicks / 20, active = Afk.effective(playTicks, afkSeconds) / 20;
        var out = new ArrayList<String>();
        out.add(whose + " time on Holy Lois: " + time(total) + " played");
        out.add("  Active " + time(active) + ", AFK " + time(Math.max(0, total - active)));
        out.add("  Fishing " + time(fishingSeconds) + " (" + fish + " fish caught), boombox music " + time(musicSeconds));
        out.add("  Walked " + km(walkedCm) + ", by boat " + km(boatCm) + ", " + kills + " mobs defeated, " + deaths + (deaths == 1 ? " death" : " deaths"));
        return out;
    }

    static String time(long seconds) { return seconds < 60 ? "0 min" : Redeem.duration(seconds); }

    static String km(long cm) {
        double km = cm / 100_000.0;
        return km >= 10 ? Math.round(km) + " km" : String.format(java.util.Locale.ROOT, "%.1f km", km);
    }
}
