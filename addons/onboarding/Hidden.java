package holylois;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.function.Predicate;

/**
 * Staff stay out of the online lists: operators and the quiet names are not counted or listed in the server list, the Tab
 * list, /list or the Tab header for other players. They stay visible in the world, and operators still see everyone.
 * The server's own tools (pregeneration, deploys, the lag watch) ping 127.0.0.1 as "localhost" and keep the real count,
 * so nothing heavy starts while staff play. The Discord bot and the stats page ping as play.holylois.com: public view.
 */
public final class Hidden {
    private static final Map<Connection, String> HOSTS = Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile MinecraftServer server;

    private Hidden() {}

    static void register() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(started -> server = started);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> server = null);
    }

    public static void rememberHost(Connection connection, String host) { HOSTS.put(connection, host == null ? "" : host); }

    /** Staff who chose /hidden off this session. A restart forgets it, so hidden stays the default. */
    static final Set<UUID> SHOWN = java.util.concurrent.ConcurrentHashMap.newKeySet();

    static boolean hidden(MinecraftServer server, NameAndId player) {
        return !SHOWN.contains(player.id()) && (Quiet.quiet(player.name()) || server.getPlayerList().isOp(player));
    }

    /** Players who are hidden unless they choose otherwise: operators and the quiet names. */
    static boolean staff(MinecraftServer server, ServerPlayer player) {
        return Quiet.listed(player.getGameProfile().name()) || server.getPlayerList().isOp(player.nameAndId());
    }

    /**
     * /hidden [on|off]: staff switch their own visibility. Off shows them in Tab, /list and the server list and tells
     * everyone they joined; on says they left and hides them again. Without an argument it toggles.
     */
    static void command(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        dispatcher.register(net.minecraft.commands.Commands.literal("hidden")
            .requires(source -> source.getPlayer() != null && staff(source.getServer(), source.getPlayer()))
            .executes(c -> set(c.getSource().getPlayerOrException(), !SHOWN.contains(c.getSource().getPlayerOrException().getUUID())))
            .then(net.minecraft.commands.Commands.literal("on").executes(c -> set(c.getSource().getPlayerOrException(), false)))
            .then(net.minecraft.commands.Commands.literal("off").executes(c -> set(c.getSource().getPlayerOrException(), true))));
    }

    static int set(ServerPlayer player, boolean show) {
        var server = player.level().getServer();
        var name = player.getGameProfile().name().toLowerCase(Locale.ROOT);
        boolean shown = SHOWN.contains(player.getUUID());
        if (show != shown) {
            var list = server.getPlayerList();
            if (!show) list.broadcastSystemMessage(net.minecraft.network.chat.Component.translatable("multiplayer.player.left", player.getDisplayName()).withStyle(net.minecraft.ChatFormatting.YELLOW), false);
            if (show) { SHOWN.add(player.getUUID()); Quiet.SHOWN.add(name); } else { SHOWN.remove(player.getUUID()); Quiet.SHOWN.remove(name); }
            // Re-send this player's Tab row; each viewer's copy is listed or unlisted by forViewer.
            list.broadcastAll(new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED), List.of(player)));
            // Vanilla join lines are filtered (the welcome is private), so this one is plain text in the same look.
            if (show) list.broadcastSystemMessage(net.minecraft.network.chat.Component.literal(player.getGameProfile().name() + " joined the game").withStyle(net.minecraft.ChatFormatting.YELLOW), false);
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(show
            ? "You are visible: Tab, /list and the server list show you. /hidden on hides you again."
            : "You are hidden from Tab, /list and the server list. /hidden off shows you.").withStyle(net.minecraft.ChatFormatting.GRAY));
        return 1;
    }

    /**
     * Server tool broadcasts (spark profiles started by the lag watch, Polymer and LuckPerms start-up lines) reach operators
     * only. Returns false when the message was handled here and must not go to everyone.
     */
    static boolean toolBroadcast(MinecraftServer server, net.minecraft.network.chat.Component message) {
        if (!toolText(message.getString())) return true;
        for (var player : server.getPlayerList().getPlayers()) if (seesAll(player)) player.sendSystemMessage(message);
        return false;
    }

    static boolean toolText(String text) {
        var line = text.strip();
        return line.startsWith("[\u26A1]") || line.contains("spark.lucko.me") || line.startsWith("[Polymer]") || line.contains("LuckPerms v")
            || line.contains("Running on Fabric - fabric@");
    }

    static boolean seesAll(ServerPlayer viewer) { return viewer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER); }

    static List<ServerPlayer> visible(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream().filter(p -> !hidden(server, p.nameAndId())).toList();
    }

    public static List<ServerPlayer> visibleTo(MinecraftServer server, ServerPlayer viewer) {
        return viewer == null || seesAll(viewer) ? server.getPlayerList().getPlayers() : visible(server);
    }

    static boolean localTool(Connection connection) {
        return connection.getRemoteAddress() instanceof InetSocketAddress address && address.getAddress() != null
            && address.getAddress().isLoopbackAddress() && "localhost".equals(HOSTS.get(connection));
    }

    /** The server-list reply without hidden players; MiniMOTD's "just 1 more" maximum follows the visible count. */
    public static ServerStatus publicStatus(Connection connection, ServerStatus status) {
        var server = Hidden.server;
        if (server == null || localTool(connection)) return status;
        int hiddenCount = server.getPlayerCount() - visible(server).size();
        return withoutStaff(status, hiddenCount, p -> hidden(server, p));
    }

    static ServerStatus withoutStaff(ServerStatus status, int hiddenCount, Predicate<NameAndId> hidden) {
        if (hiddenCount <= 0 || status.players().isEmpty()) return status;
        var players = status.players().get();
        int online = Math.max(0, players.online() - hiddenCount);
        int max = players.max() == players.online() + 1 ? online + 1 : players.max();
        var sample = players.sample().stream().filter(p -> !hidden.test(p)).toList();
        return new ServerStatus(status.description(), Optional.of(new ServerStatus.Players(max, online, sample)), status.version(), status.favicon(), status.enforcesSecureChat());
    }

    /** Tab list entries of hidden players arrive unlisted for viewers who are not operators. */
    public static Packet<?> forViewer(Packet<?> packet, ServerPlayer viewer) {
        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket info) || viewer == null
            || !info.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED) || seesAll(viewer)) return packet;
        var server = viewer.level().getServer();
        var entries = unlisted(info.entries(), viewer.getUUID(), p -> hidden(server, p));
        if (entries == null) return packet;
        var copy = new ClientboundPlayerInfoUpdatePacket(info.actions(), List.of());
        ((holylois.mixins.PlayerInfoEntriesAccessor) copy).holyLoisSetEntries(entries);
        return copy;
    }

    /** The entries with hidden players unlisted (the viewer always keeps their own row), or null when nothing changes. */
    static List<ClientboundPlayerInfoUpdatePacket.Entry> unlisted(List<ClientboundPlayerInfoUpdatePacket.Entry> entries, UUID viewer, Predicate<NameAndId> hidden) {
        boolean changed = false;
        var out = new ArrayList<ClientboundPlayerInfoUpdatePacket.Entry>(entries.size());
        for (var e : entries) {
            if (e.listed() && e.profile() != null && !e.profileId().equals(viewer) && hidden.test(new NameAndId(e.profileId(), e.profile().name()))) {
                e = new ClientboundPlayerInfoUpdatePacket.Entry(e.profileId(), e.profile(), false, e.latency(), e.gameMode(), e.displayName(), e.showHat(), e.listOrder(), e.chatSession());
                changed = true;
            }
            out.add(e);
        }
        return changed ? out : null;
    }
}
