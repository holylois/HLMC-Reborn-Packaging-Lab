package holylois.boombox;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.world.MinimapWorld;

/**
 * [Waypoint] in discovery lines (onboarding Discoveries): "/hlwaypoint <dimension> <x> <y> <z> <name>" never reaches the server;
 * ClickCommandMixin hands it here and it becomes a gold waypoint in Xaero's Minimap for that dimension. Only loaded with Xaero.
 */
final class XaeroWaypoints {
    private XaeroWaypoints() {}

    static void add(String arguments) {
        var player = Minecraft.getInstance().player;
        String[] parts = arguments.strip().split(" ", 5);
        if (player == null) return;
        if (parts.length < 5) { player.sendSystemMessage(Component.literal("Waypoint: missing place or name.")); return; }
        Identifier dimension = Identifier.tryParse(parts[0]);
        int x, y, z;
        try { x = Integer.parseInt(parts[1]); y = Integer.parseInt(parts[2]); z = Integer.parseInt(parts[3]); }
        catch (NumberFormatException error) { player.sendSystemMessage(Component.literal("Waypoint: bad coordinates.")); return; }
        String name = parts[4].length() > 40 ? parts[4].substring(0, 40) : parts[4];
        var session = BuiltInHudModules.MINIMAP.getCurrentSession();
        var root = session == null ? null : session.getWorldManager().getCurrentRootContainer();
        if (root == null || dimension == null) { player.sendSystemMessage(Component.literal("Waypoint: the minimap is not ready yet, try again in a moment.")); return; }
        for (MinimapWorld world : root.getAllWorldsIterable()) {
            var dim = world.getDimId();
            if (!dimension.equals(dim != null ? dim.identifier() : world.getContainer().getEquivalentDimId())) continue;
            var set = world.getCurrentWaypointSet();
            if (set == null) continue;
            for (Waypoint point : set.getWaypoints())
                if (point.getX() == x && point.getZ() == z && point.getName().equals(name)) {
                    player.sendSystemMessage(Component.literal("Waypoint " + name + " is already on your map."));
                    return;
                }
            String initials = name.replaceAll("[^A-Za-z0-9]", "");
            set.add(new Waypoint(x, y, z, name, initials.isEmpty() ? "?" : initials.substring(0, Math.min(2, initials.length())).toUpperCase(java.util.Locale.ROOT), WaypointColor.GOLD));
            try { session.getWorldManagerIO().saveWorld(world); } catch (Exception ignored) { /* Xaero saves again on its own */ }
            player.sendSystemMessage(Component.literal("Waypoint added: " + name + " (" + x + ", " + y + ", " + z + ")").withStyle(net.minecraft.ChatFormatting.GREEN));
            return;
        }
        player.sendSystemMessage(Component.literal("Waypoint: visit that dimension once, then click again."));
    }
}
