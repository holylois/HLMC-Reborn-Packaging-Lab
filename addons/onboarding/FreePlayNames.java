package holylois;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.PlayersCache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Free-play name rule (owner 2026-10-18). EasyAuth (premium-auto-login, check-offline-players-with-online-usernames) lets the
 * real owner of a bought name claim it: their join is verified by Mojang and the name becomes an online account. The old
 * free-play character still sits under that name's offline UUID; on the owner's first verified join it is moved into
 * world/holylois/private-account-backups (nothing is deleted) and online operators are told. A free-play name registered
 * while nobody owned it stays offline in EasyAuth, so it is never claimed this way.
 */
final class FreePlayNames {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("HolyLois");
    private static final List<String> SKIP = List.of("region", "entities", "poi", "data", "DIM-1", "DIM1", "dimensions", "holylois", "bluemap", "datapacks");

    private FreePlayNames() {}

    static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.getPlayer();
            try {
                PlayerEntryV1 entry = PlayersCache.get(player.getGameProfile().name());
                if (entry == null || entry.onlineAccount != PlayerEntryV1.OnlineAccount.TRUE) return;
                var moved = moveToBackup(server.getWorldPath(LevelResource.ROOT), player.getGameProfile().name(), player.getUUID(), Instant.now());
                if (moved != null) tell(server, player, moved);
            } catch (IOException | RuntimeException | LinkageError error) {
                LOG.warn("Free-play name check failed for {}", player.getGameProfile().name(), error);
                handler.disconnect(Component.literal("Your account files need a staff recovery check. Please contact Holy Lois support."));
            }
        });
    }

    static UUID offline(String name) { return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)); }

    /** Moves every file named after the name's offline UUID out of the world's per-player folders. Returns the backup folder, or null if there was nothing to move. */
    static synchronized Path moveToBackup(Path world, String name, UUID owner, Instant now) throws IOException {
        world = world.toAbsolutePath().normalize();
        if (!name.matches("[A-Za-z0-9_]{3,16}")) throw new IOException("Invalid account name");
        var old = offline(name);
        if (old.equals(owner)) return null;
        var backups = world.resolve("holylois/private-account-backups");
        FileQuarantine.safe(world, backups);
        if (Files.isDirectory(backups)) {
            try (var prior = Files.list(backups)) {
                for (var dir : prior.filter(p -> p.getFileName().toString().startsWith("free-play-" + name + "-")).toList()) {
                    if (Files.exists(dir.resolve("transaction.json")) && FileQuarantine.resume(world, dir, owner.toString())) return dir;
                    if (Files.exists(dir.resolve("transaction.pending"))) throw new IOException("Incomplete account journal needs staff review");
                }
            }
        }
        // The free-play player may have joined as "steve" before the owner of "Steve" bought it: offline UUIDs depend on case.
        var olds = new java.util.LinkedHashSet<UUID>(); olds.add(old); olds.addAll(cachedSpellings(world, name)); olds.remove(owner);
        var prefixes = olds.stream().map(id -> id + ".").toList();
        var files = new ArrayList<Path>();
        try (var dirs = Files.list(world)) {
            for (var dir : dirs.filter(Files::isDirectory).filter(d -> !SKIP.contains(d.getFileName().toString())).toList())
                try (var inside = Files.list(dir)) { inside.filter(Files::isRegularFile).filter(f -> prefixes.stream().anyMatch(f.getFileName().toString()::startsWith)).forEach(files::add); }
        }
        if (files.isEmpty()) return null;
        var backup = world.resolve("holylois/private-account-backups/free-play-" + name + "-" + now.getEpochSecond());
        FileQuarantine.begin(world, backup, owner.toString(), files);
        Files.writeString(backup.resolve("moved.txt"), "Free-play character of " + name + " (offline UUID " + old + ") moved to backup at " + now
            + " because the bought account " + owner + " joined with this name.\n" + files.size() + " files, original folders kept below.\n");
        LOG.info("Free-play character of {} moved to {} ({} files): the bought account owner joined", name, backup, files.size());
        return backup;
    }

    /** Offline UUIDs of other spellings of the name in the server's usercache.json (same name, different case). */
    static List<UUID> cachedSpellings(Path world, String name) {
        var cache = world.toAbsolutePath().getParent() == null ? null : world.toAbsolutePath().getParent().resolve("usercache.json");
        if (cache == null || !Files.isRegularFile(cache)) return List.of();
        try {
            var out = new ArrayList<UUID>();
            for (var entry : com.google.gson.JsonParser.parseString(Files.readString(cache)).getAsJsonArray()) {
                var spelled = entry.getAsJsonObject().get("name").getAsString();
                if (spelled.equalsIgnoreCase(name) && !spelled.equals(name)) out.add(offline(spelled));
            }
            return out;
        } catch (IOException | RuntimeException error) { return List.of(); }
    }

    private static void tell(net.minecraft.server.MinecraftServer server, ServerPlayer owner, Path backup) {
        var text = Component.literal("Free-play character of " + owner.getGameProfile().name() + " moved to backup: the bought account owner joined. Folder: "
            + server.getWorldPath(LevelResource.ROOT).relativize(backup)).withStyle(ChatFormatting.GRAY);
        for (var op : server.getPlayerList().getPlayers()) if (op != owner && server.getPlayerList().isOp(op.nameAndId())) op.sendSystemMessage(text);
    }
}
