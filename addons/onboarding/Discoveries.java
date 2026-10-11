package holylois;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import holylois.boombox.JournalState;
import holylois.boombox.JournalRequest;
import holylois.boombox.PartySupport;
import holylois.boombox.FishCaught;
import holylois.boombox.FishData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Discoveries and world firsts (owner 2026-10-11). Every notable structure instance is remembered once. The first of each kind
 * (by its shown name) is a gold WORLD FIRST for everyone; later ones are numbered ("Ice Spire #3") with the biome and where it
 * is, so two "Big Ice Structure" finds no longer look like the same thing. Each line has [Map] (BlueMap at the spot) and
 * [Waypoint] (adds it to Xaero's minimap on the clicking player's own client). Boss kills and Runeforged elites/levels have
 * their own world firsts. Villages and small ruins stay quiet. /discoveries lists them, newest first.
 */
public final class Discoveries {
    private static final Logger LOG = LoggerFactory.getLogger("HolyLois");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    static final String MAP = "https://map.holylois.com/";

    public static final class Found {
        public String key, structure, name, player, dim, biome;
        public long time;
        public int x, y, z, number;
        public boolean first;
        public UUID uuid;
    }
    /** A boss or elite world first: key "boss:minecraft:ender_dragon", "elite", "level:50"... */
    public static final class First { public String key, what, player, dim; public long time; public int x, y, z, level; }
    public static final class State {
        public int version = 3;
        public List<Found> found = new ArrayList<>(); public List<First> firsts = new ArrayList<>();
        public Map<UUID,JournalBook> journals = new HashMap<>();
    }

    private static final Map<String, String> VANILLA = Map.ofEntries(
        Map.entry("ancient_city", "Ancient City"), Map.entry("trial_chambers", "Trial Chambers"),
        Map.entry("stronghold", "Stronghold"), Map.entry("mansion", "Woodland Mansion"),
        Map.entry("monument", "Ocean Monument"), Map.entry("fortress", "Nether Fortress"),
        Map.entry("bastion_remnant", "Bastion"), Map.entry("end_city", "End City"),
        Map.entry("pillager_outpost", "Pillager Outpost"), Map.entry("desert_pyramid", "Desert Temple"),
        Map.entry("jungle_pyramid", "Jungle Temple"), Map.entry("trail_ruins", "Trail Ruins"));
    // Small, very common or village-like modded structures. Anything else from a structure mod is announced.
    private static final Pattern QUIET = Pattern.compile(
        "(village|hamlet|^well_|camp|small_|firewatch|wreckage|mimic|cave_hut|underground_house|wild_ruin|witch_hut"
        + "|desert_ruins|jungle_ruins|^remnant_(?!taiga_castle|big_remnant))");

    // Small crypts count as dungeons even though other small_ structures stay quiet.
    private static final Pattern CRYPTS = Pattern.compile("^small_(creeping|undead)_crypt$");

    // Bosses get a world first for their first kill (the c:bosses tag adds modded ones).
    private static final Map<String, String> BOSSES = Map.of("minecraft:ender_dragon", "the Ender Dragon", "minecraft:wither", "the Wither",
        "minecraft:warden", "a Warden", "minecraft:elder_guardian", "an Elder Guardian");
    private static final int[] LEVELS = {50, 75, 100};

    private final Map<String, Found> found = new HashMap<>();
    private final Map<String, First> firsts = new LinkedHashMap<>();
    private final Map<String, Integer> counts = new HashMap<>();
    private final Map<UUID,JournalBook> journals = new HashMap<>();
    private final Map<UUID,Long> nextPage = new HashMap<>();
    private boolean dirty;
    private Path file;

    void registerJournal() {
        ServerPlayNetworking.registerGlobalReceiver(JournalRequest.TYPE,(request,context)->open(context.player(),request));
        FishCaught.EVENT.register((player,fish)->{
            if(file==null||!PartySupport.ready(player))return;
            book(player).caught(BuiltInRegistries.ITEM.getKey(fish.getItem()).toString(),fish.getCount(),FishData.kilograms(fish),FishData.rarity(fish),
                player.level().dimension().identifier().toString(),player.getBlockX(),player.getBlockY(),player.getBlockZ(),now());
            dirty=true;
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{if(dirty&&server.getTickCount()%400==0)save();});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server->{if(dirty)save();});
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->nextPage.remove(handler.player.getUUID()));
    }
    private static long now(){return System.currentTimeMillis()/1000;}
    private JournalBook book(ServerPlayer player){return journals.computeIfAbsent(player.getUUID(),id->new JournalBook());}
    int open(ServerPlayer player,JournalRequest request) {
        if(file==null||!PartySupport.ready(player))return 0;
        long now=System.currentTimeMillis();
        if(now<nextPage.getOrDefault(player.getUUID(),0L))return 0;
        nextPage.put(player.getUUID(),now+150);
        if(ServerPlayNetworking.canSend(player,JournalState.TYPE))ServerPlayNetworking.send(player,snapshot(player,request));
        else player.sendSystemMessage(Component.literal("Update your pack to open the Holy Lois Journal.").withStyle(ChatFormatting.YELLOW));
        return 1;
    }
    JournalState snapshot(ServerPlayer player,JournalRequest request){
        var entries=new ArrayList<JournalState.Entry>();
        var own=book(player);
        for(var f:found.values()){
            // Legacy first discoveries can be recovered by name; new visits use the stable UUID.
            if((player.getUUID().equals(f.uuid)||f.uuid==null&&player.getGameProfile().name().equalsIgnoreCase(f.player))&&own.visit(f.key,f.time))dirty=true;
            if(request.tab()==0 ? !f.first : !own.places.containsKey(f.key))continue;
            entries.add(new JournalState.Entry(f.key,JournalState.PLACE,f.name+(f.first?"":" #"+f.number),StatsVisibility.display(f.player),
                f.dim==null?"minecraft:overworld":f.dim,f.x,f.y,f.z,request.tab()==0?f.time:own.places.get(f.key),1,0,0,0,true,f.first));
        }
        if(request.tab()==0){
            for(var f:firsts.values()){
                boolean mob=f.key.startsWith("mob:");
                if(mob&&firsts.containsKey("boss:"+f.key.substring(4)))continue;
                entries.add(new JournalState.Entry(f.key,mob?JournalState.KILL:JournalState.FEAT,mob?f.key.substring(4):f.what,StatsVisibility.display(f.player),
                    f.dim,f.x,f.y,f.z,f.time,1,0,0,f.level,true,true));
            }
        }else{
            for(var f:own.fish.values())entries.add(new JournalState.Entry("fish:"+f.species,JournalState.FISH,f.species,"",f.dim,f.x,f.y,f.z,f.time,f.count,f.heaviest,f.rarities,0,true,false));
            for(var type:BuiltInRegistries.ENTITY_TYPE){
                long count=player.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED,type);
                if(count<=0)continue;
                String id=BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();var k=own.kills.get(id);
                if(id.equals("minecraft:player"))continue;
                entries.add(new JournalState.Entry("kill:"+id,JournalState.KILL,id,"",k==null?"":k.dim,k==null?0:k.x,k==null?0:k.y,k==null?0:k.z,
                    k==null?0:k.time,count,0,0,k==null?0:k.level,k!=null,false));
            }
        }
        return JournalBook.page(entries,request);
    }

    /** Display name for a structure id, or null when it should not be announced. */
    static String name(String id) {
        int colon = id.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : id.substring(0, colon), path = id.substring(colon + 1);
        if (namespace.equals("minecraft")) return VANILLA.get(path);
        path = path.substring(path.lastIndexOf('/') + 1);
        // Epic Dungeons are underground dungeons of every size: all of them are worth an announcement (small_ would hide them).
        if (namespace.equals("epic")) return path.equals("large_dungeon") ? "Large Plains Dungeon" : title(path);
        if (CRYPTS.matcher(path).find()) return title(path);
        if (QUIET.matcher(path).find()) return null;
        if (path.startsWith("tavern_")) return "Tavern";
        if (path.startsWith("pillager_outpost")) return "Pillager Outpost";
        if (path.startsWith("nether_skeleton_tower")) return "Skeleton Tower";
        if (path.contains("trial_dungeon")) return "Trial Dungeon";
        if (path.startsWith("surface_dungeon")) return "Surface Dungeon";
        if (path.startsWith("shrine_combat")) return "Combat Shrine";
        if (path.startsWith("shrine_biome")) return "Biome Shrine";
        if (path.startsWith("cave_chamber")) return "Cave Chamber";
        if (path.startsWith("remnant_big_remnant")) return "Big Ruin";
        if (path.equals("remnant_taiga_castle")) return "Ruined Taiga Castle";
        return title(path.replaceAll("_\\d+$", ""));
    }

    static String title(String path) {
        var out = new StringBuilder();
        for (String word : path.split("_")) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    static String article(String name) { return "AEIOU".indexOf(Character.toUpperCase(name.charAt(0))) >= 0 ? "an" : "a"; }

    void load(MinecraftServer server, Path worldRoot) {
        found.clear();firsts.clear();counts.clear();journals.clear();nextPage.clear();dirty=false;
        file = worldRoot.resolve("holylois/discoveries.json");
        try {
            if (Files.exists(file)) {
                var state = JSON.fromJson(Files.readString(file), State.class);
                if (state != null && state.found != null) {
                    state.found.sort(Comparator.comparingLong(f -> f.time));
                    boolean upgrade = state.version < 2;
                    for (var entry : state.found) {
                        if (upgrade) upgradeEntry(entry);
                        found.put(entry.key, entry);
                        counts.merge(entry.name, 1, Integer::sum);
                    }
                    if (upgrade) LOG.info("Holy Lois discoveries: numbered {} older finds (first of each kind = world first)", state.found.size());
                }
                if (state != null && state.firsts != null) for (var first : state.firsts) firsts.put(first.key, first);
                if(state!=null&&state.journals!=null)journals.putAll(state.journals);
                if (state != null && state.version < 3) save();
            }
            LOG.info("Holy Lois discoveries: {} structures found so far, {} world firsts", found.size(),
                found.values().stream().filter(f -> f.first).count() + firsts.size());
        } catch (Exception error) { throw new IllegalStateException("Cannot read discoveries; refusing to overwrite saved journal records",error); }
    }

    /** Version 1 kept one entry per find: number them by time, the first of each name is the world first, place from the key. */
    private void upgradeEntry(Found entry) {
        entry.number = counts.getOrDefault(entry.name, 0) + 1;
        entry.first = entry.number == 1;
        String[] parts = entry.key.split(Pattern.quote("|"));
        entry.dim = parts.length > 0 ? parts[0] : "minecraft:overworld";
        if (parts.length > 2) {
            String[] chunk = parts[2].split(",");
            try { entry.x = Integer.parseInt(chunk[0]) * 16 + 8; entry.z = Integer.parseInt(chunk[1]) * 16 + 8; entry.y = 64; }
            catch (RuntimeException ignored) { /* an odd old key: the line just points at 0, 0 */ }
        }
    }

    /** Called every two seconds for each authenticated player. */
    void check(MinecraftServer server, ServerPlayer player) {
        if (file == null || player.isSpectator() || !PartySupport.ready(player)) return;
        var level = player.level();
        var manager = level.structureManager();
        var pos = player.blockPosition();
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (var structure : manager.getAllStructuresAt(pos).keySet()) {
            var id = registry.getKey(structure);
            if (id == null) continue;
            String name = name(id.toString());
            if (name == null) continue;
            var start = manager.getStructureWithPieceAt(pos.getX(), pos.getY(), pos.getZ(), structure);
            if (start == null || !start.isValid()) continue;
            String key = level.dimension().identifier() + "|" + id + "|" + start.getChunkPos().x() + "," + start.getChunkPos().z();
            if(book(player).visit(key,now()))dirty=true;
            if (found.containsKey(key)) continue;
            var entry = new Found();
            entry.key = key; entry.structure = id.toString(); entry.name = name;
            entry.player = player.getGameProfile().name(); entry.time = System.currentTimeMillis() / 1000;
            entry.uuid = player.getUUID();
            entry.dim = level.dimension().identifier().toString();
            entry.x = pos.getX(); entry.y = pos.getY(); entry.z = pos.getZ();
            entry.biome = level.getBiome(pos).unwrapKey().map(k -> k.identifier().toString()).orElse("");
            entry.number = counts.merge(name, 1, Integer::sum);
            entry.first = entry.number == 1;
            found.put(key, entry);
            save();
            if (entry.first) {
                server.getPlayerList().broadcastSystemMessage(worldFirst(entry.player, Component.literal(" discovered the first " + name + where(entry, true) + "!")
                    .withStyle(ChatFormatting.YELLOW)).append(links(entry.dim, entry.x, entry.y, entry.z, name)), false);
                ding(server);
            } else {
                server.getPlayerList().broadcastSystemMessage(Component.literal("◇ ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(entry.player).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" found ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(name + " #" + entry.number).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(where(entry, false)).withStyle(ChatFormatting.GRAY))
                    .append(links(entry.dim, entry.x, entry.y, entry.z, name + " #" + entry.number)), false);
                level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.4f);
            }
            Achievements.award(server, player, "explore/discoverer", "done");
            LOG.info("Discovery: {} found {} #{} ({}) at {} {} {}", entry.player, name, entry.number, id, entry.x, entry.y, entry.z);
        }
    }

    /** Boss and Runeforged firsts, from the AFTER_DEATH hook. Credit goes to the player who earned the kill. */
    void killed(MinecraftServer server, LivingEntity entity) {
        if (file == null || !(entity.getKillCredit() instanceof ServerPlayer player) || !PartySupport.ready(player)) return;
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        int[] rune = Runeforged.levelAndElite(entity);
        int level=rune==null?0:rune[0];
        book(player).killed(type,level,entity.level().dimension().identifier().toString(),entity.getBlockX(),entity.getBlockY(),entity.getBlockZ(),now());
        dirty=true;
        String boss = BOSSES.get(type);
        if (boss == null && entity.getType().builtInRegistryHolder().is(BOSS_TAG)) boss = entity.getName().getString();
        if (boss != null) first(server, player, entity, "boss:" + type, "slew " + boss);
        var mobFirst=rememberMob(type,player.getGameProfile().name(),entity.level().dimension().identifier().toString(),entity.getBlockX(),entity.getBlockY(),entity.getBlockZ(),now(),level);
        if(mobFirst!=null){
            if(boss==null)server.getPlayerList().broadcastSystemMessage(Component.literal("✦ World first: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(StatsVisibility.display(mobFirst.player)+" defeated the first "))
                .append(entity.getType().getDescription()).append(Component.literal(".")),false);
        }
        if (rune == null) return;
        String mob = entity.getName().getString();
        if (rune[1] > 0) first(server, player, entity, "elite", "slew the first Runeforged elite (" + mob + ", level " + rune[0] + ")");
        for (int needed : LEVELS)
            if (rune[0] >= needed) first(server, player, entity, "level:" + needed, "slew the first level " + needed + "+ monster (" + mob + ", level " + rune[0] + ")");
    }

    private static final TagKey<EntityType<?>> BOSS_TAG = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", "bosses"));

    /** First credit never changes when a later kill raises the record level. */
    First rememberMob(String type,String player,String dimension,int x,int y,int z,long time,int level){
        String key="mob:"+type;var prior=firsts.get(key);
        if(prior!=null){if(level>prior.level){prior.level=level;dirty=true;}return null;}
        var first=new First();first.key=key;first.what="slew the first "+title(type.substring(type.indexOf(':')+1));
        first.player=player;first.dim=dimension;first.x=x;first.y=y;first.z=z;first.time=time;first.level=level;
        firsts.put(key,first);dirty=true;return first;
    }

    private void first(MinecraftServer server, ServerPlayer player, LivingEntity entity, String key, String what) {
        if (firsts.containsKey(key)) return;
        var first = new First();
        first.key = key; first.what = what; first.player = player.getGameProfile().name(); first.time = System.currentTimeMillis() / 1000;
        first.dim = entity.level().dimension().identifier().toString();
        first.x = entity.getBlockX(); first.y = entity.getBlockY(); first.z = entity.getBlockZ();
        firsts.put(key, first);
        save();
        server.getPlayerList().broadcastSystemMessage(worldFirst(first.player, Component.literal(" " + what + "!").withStyle(ChatFormatting.YELLOW)), false);
        ding(server);
        LOG.info("World first: {} {} at {} {} {}", first.player, what, first.x, first.y, first.z);
    }

    /** The challenge chime for every player, each only hearing their own. */
    private static void ding(MinecraftServer server) {
        for (var other : server.getPlayerList().getPlayers())
            other.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE), SoundSource.PLAYERS, other.getX(), other.getY(), other.getZ(), 0.45f, 1.1f, other.getRandom().nextLong()));
    }

    private static MutableComponent worldFirst(String player, Component rest) {
        return Component.literal("✦ WORLD FIRST ✦ ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            .append(Component.literal(StatsVisibility.display(player)).withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(false)))
            .append(rest.copy().withStyle(style -> style.withBold(false)));
    }

    /** " in a Frozen Peaks biome, 2.4k blocks NE of spawn" (repeats: " · Frozen Peaks, 2.4k blocks NE of spawn"). */
    static String where(Found entry, boolean full) {
        String biome = entry.biome == null || entry.biome.isEmpty() ? "" : title(entry.biome.substring(entry.biome.indexOf(':') + 1));
        if (entry.dim != null && entry.dim.endsWith("the_nether")) return " in the Nether" + (biome.isEmpty() ? "" : " (" + biome + ")");
        if (entry.dim != null && entry.dim.endsWith("the_end")) return " in the End";
        double distance = Math.hypot(entry.x, entry.z);
        String far = distance < 1000 ? String.valueOf(Math.round(distance / 10) * 10) : String.format(Locale.ROOT, "%.1fk", distance / 1000);
        String[] compass = {"E", "SE", "S", "SW", "W", "NW", "N", "NE"};
        String dir = compass[(int) Math.floorMod(Math.round(Math.toDegrees(Math.atan2(entry.z, entry.x)) / 45), 8L)];
        String place = biome.isEmpty() ? "" : full ? " in " + article(biome) + " " + biome + " biome" : " · " + biome;
        return place + ", " + far + " blocks " + dir + " of spawn";
    }

    /** [Map] opens BlueMap at the spot, [Waypoint] adds a Xaero waypoint on the clicking player's client (extras). */
    static MutableComponent links(String dim, int x, int y, int z, String name) {
        String map = dim == null || dim.endsWith("overworld") ? "world" : dim.endsWith("the_nether") ? "world_the_nether" : dim.endsWith("the_end") ? "world_the_end" : null;
        var out = Component.literal("");
        if (map != null) out.append(Component.literal(" [Map]").withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(false)
            .withClickEvent(new ClickEvent.OpenUrl(URI.create(MAP + "#" + map + ":" + x + ":" + y + ":" + z + ":120:0:0:0:1:flat")))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Open the live map at " + x + ", " + z)))));
        String safe = name.replaceAll("[^A-Za-z0-9 #+-]", "").strip();
        out.append(Component.literal(" [Waypoint]").withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(false)
            .withClickEvent(new ClickEvent.RunCommand("/hlwaypoint " + (dim == null ? "minecraft:overworld" : dim) + " " + x + " " + y + " " + z + " " + safe))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Add " + safe + " to your minimap (" + x + ", " + y + ", " + z + ")")))));
        return out;
    }

    /** /discoveries [page] and /discoveries mine [page]: newest first, world firsts in gold, ten per page, with map links. */
    int list(ServerPlayer viewer, boolean mine, int page) {
        var all = new ArrayList<Found>(found.values());
        if (mine) all.removeIf(f -> !viewer.getGameProfile().name().equalsIgnoreCase(f.player));
        all.sort(Comparator.comparingLong((Found f) -> f.time).reversed());
        int pages = Math.max(1, (all.size() + 9) / 10);
        int shown = Math.max(1, Math.min(page, pages));
        var head = Component.literal((mine ? "Your discoveries" : "Discoveries") + " (" + all.size() + ", page " + shown + "/" + pages + ")")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        if (!mine && !firsts.isEmpty()) head.append(Component.literal("  " + firsts.size() + " boss/elite firsts").withStyle(style -> style.withColor(ChatFormatting.YELLOW).withBold(false)));
        viewer.sendSystemMessage(head);
        if (all.isEmpty()) viewer.sendSystemMessage(Component.literal("Nothing yet. Notable structures are announced when someone walks in.").withStyle(ChatFormatting.GRAY));
        for (var f : all.subList((shown - 1) * 10, Math.min(all.size(), shown * 10))) {
            var line = Component.literal(f.first ? "✦ " : "◇ ").withStyle(f.first ? ChatFormatting.GOLD : ChatFormatting.GRAY)
                .append(Component.literal(f.name + (f.first ? " (world first)" : " #" + f.number)).withStyle(f.first ? ChatFormatting.GOLD : ChatFormatting.WHITE))
                .append(Component.literal(" by " + f.player + where(f, false)).withStyle(ChatFormatting.GRAY));
            viewer.sendSystemMessage(line.append(links(f.dim, f.x, f.y, f.z, f.first ? f.name : f.name + " #" + f.number)));
        }
        if (!mine && shown == 1) for (var b : firsts.values())
            viewer.sendSystemMessage(Component.literal("✦ ").withStyle(ChatFormatting.GOLD).append(Component.literal(b.player + " " + b.what).withStyle(ChatFormatting.GOLD)));
        if (shown < pages) viewer.sendSystemMessage(Component.literal("[Next page]").withStyle(style -> style.withColor(ChatFormatting.AQUA)
            .withClickEvent(new ClickEvent.RunCommand("/discoveries " + (mine ? "mine " : "") + (shown + 1)))));
        return all.size();
    }

    /** /structures: which structures the player is standing in, with their ids, so new ones can be named or announced. */
    static int here(ServerPlayer player) {
        var level = player.level();
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var pos = player.blockPosition();
        var text = Component.literal("Structures here: ").withStyle(ChatFormatting.GOLD);
        int count = 0;
        for (var structure : level.structureManager().getAllStructuresAt(pos).keySet()) {
            var start = level.structureManager().getStructureWithPieceAt(pos.getX(), pos.getY(), pos.getZ(), structure);
            var id = registry.getKey(structure);
            if (id == null || start == null || !start.isValid()) continue;
            String name = name(id.toString());
            text.append(Component.literal((count++ == 0 ? "" : ", ") + (name == null ? id + " (not announced)" : name + " (" + id + ")"))
                .withStyle(ChatFormatting.YELLOW));
        }
        if (count == 0) text.append(Component.literal("none").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(text);
        return count;
    }

    private void save() {
        try {
            var state = new State();
            state.found.addAll(found.values());
            state.found.sort(Comparator.comparingLong(f -> f.time));
            state.firsts.addAll(firsts.values());
            state.journals.putAll(journals);
            Files.createDirectories(file.getParent());
            var temporary = file.resolveSibling("discoveries.json.tmp");
            Files.writeString(temporary, JSON.toJson(state));
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            dirty=false;
        } catch (Exception error) { LOG.warn("Cannot save discoveries", error); }
    }

    /** Runeforged monster level and elite flag by reflection (the mod is optional for this add-on). */
    static final class Runeforged {
        private static java.lang.reflect.Method level, elite;
        private static boolean missing;

        /** {level, elite ? 1 : 0}, or null when Runeforged is absent or the entity has no level. */
        static int[] levelAndElite(LivingEntity entity) {
            if (missing) return null;
            try {
                if (level == null) {
                    var access = Class.forName("com.ultimatearpg.monster.ArpgMonsterLevelAccess");
                    level = access.getMethod("ultimatearpg$getMonsterLevel");
                    elite = access.getMethod("ultimatearpg$isEliteMonster");
                }
                if (!level.getDeclaringClass().isInstance(entity)) return null;
                int value = (int) level.invoke(entity);
                return value <= 0 ? null : new int[] {value, (boolean) elite.invoke(entity) ? 1 : 0};
            } catch (ReflectiveOperationException | LinkageError error) { missing = true; return null; }
        }
    }
}
