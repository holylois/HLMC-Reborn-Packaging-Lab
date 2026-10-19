package holylois.gametest;

import net.minecraft.world.inventory.Slot;

import holylois.boombox.DeathLootGone;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.world.MinimapWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs inside a real game with the full client mod set plus our add-ons (gradlew :dev:runClientGameTest). Every test runs
 * even if an earlier one fails; screenshots land in run/gametest/screenshots.
 */
public final class HolyLoisClientTests implements FabricClientGameTest {
    interface Test { void run(ClientGameTestContext context, TestSingleplayerContext world) throws Exception; }

    /**
     * Ends with an immediate exit instead of closing the world: with the full pack, Fabric's test world close deadlocks
     * (client waits for the integrated server, which waits for the test phase). Exit code 0 = all passed, 1 = a failure.
     */
    @Override public void runTest(ClientGameTestContext context) {
        List<String> failed = new ArrayList<>();
        try {
            context.takeScreenshot("01-title");
            journalFixture();
            holylois.StrongestJournalChecks.register();
            var world = context.worldBuilder().create();
            world.getConnection().waitForChunksRender();
            context.takeScreenshot("02-world");
            run(failed, "fresh installation defaults", context, world, FreshDefaultsChecks::run);
            run(failed, "managed resource packs", context, world, ManagedPackChecks::run);
            run(failed, "inventory opens", context, world, HolyLoisClientTests::inventoryOpens);
            run(failed, "enter submits the login form", context, world, HolyLoisClientTests::enterSubmitsLogin);
            run(failed, "menus and daily card", context, world, HolyLoisClientTests::menusAndDailyCard);
            run(failed, "fish carry, shine and catch card", context, world, HolyLoisClientTests::fishCarryShineCard);
            run(failed, "fish fillet cuts", context, world, HolyLoisClientTests::filletCuts);
            run(failed, "arm animations", context, world, HolyLoisClientTests::armAnimations);
            run(failed, "boombox and water", context, world, HolyLoisClientTests::boomboxKeepsWaterSources);
            run(failed, "chest lid stays shut", context, world, HolyLoisClientTests::chestLidStaysShut);
            run(failed, "death marker cleanup", context, world, HolyLoisClientTests::deathMarkerGoesWhenLootIsGone);
            run(failed, "trophy fish look", context, world, HolyLoisClientTests::trophyFishLook);
            run(failed, "boombox model", context, world, HolyLoisClientTests::boomboxModel);
            run(failed, "boombox hand closeup", context, world, HolyLoisClientTests::boomboxHand);
            run(failed, "boombox held notes", context, world, HolyLoisClientTests::boomboxHeldNotes);
            run(failed, "vanilla held fish and placed notes", context, world, HeldFishChecks::run);
            run(failed, "lantern swing", context, world, HolyLoisClientTests::lanternSwing);
            run(failed, "holy lootbox", context, world, HolyLoisClientTests::holyLootbox);
            run(failed, "invisible frame keeps its frame", context, world, HolyLoisClientTests::invisibleFrameDrops);
            run(failed, "offhand boombox is not placed", context, world, HolyLoisClientTests::offhandBoomboxStays);
            run(failed, "death loot goes back to its slots", context, world, HolyLoisClientTests::deathLootBackToSlots);
            run(failed, "music in your ears during the cinematic", context, world, HolyLoisClientTests::musicInEarsDuringCinematic);
            run(failed, "fishing cinematic angles and quiet soundtrack", context, world, HolyLoisClientTests::fishingCinematicAngles);
            run(failed, "fish picked up and put back in the same slot", context, world, HolyLoisClientTests::fishSameSlot);
            run(failed, "held maps", context, world, HolyLoisClientTests::heldMaps);
            run(failed, "water by camera height", context, world, HolyLoisClientTests::waterByHeight);
            run(failed, "item cards", context, world, HolyLoisClientTests::itemCards);
            run(failed, "holy lois journal", context, world, HolyLoisClientTests::journal);
            run(failed, "strongest journal record and zombie preview", context, world, holylois.StrongestJournalChecks::run);
            run(failed, "journal fresh animations", context, world, holylois.FreshAnimationJournalChecks::run);
            run(failed, "liteminer operator hint hidden", context, world, HolyLoisClientTests::liteminerHint);
            run(failed, "fish records and catch milestones", context, world, holylois.CollectionChecks::run);
            run(failed, "smelting enchantment compatibility", context, world, holylois.SmeltingChecks::run);
            run(failed, "cards over the cinematic camera", context, world, HolyLoisClientTests::cinematicCards);
            run(failed, "hooked school fish arrives as an item", context, world, HolyLoisClientTests::schoolCatch);
            run(failed, "shoal catch arrives as an item", context, world, HolyLoisClientTests::shoalCatch);
            run(failed, "a bite catches the fish swimming at the bobber", context, world, HolyLoisClientTests::seenCatch);
            run(failed, "one rarity system names every item in our tiers", context, world, HolyLoisClientTests::holyRarity);
            run(failed, "pufferfish thorns", context, world, HolyLoisClientTests::pufferThorns);
            run(failed, "tier tooltip screenshots", context, world, HolyLoisClientTests::tierTooltipShots);
            run(failed, "persistent shoal fish and swimming sizes", context, world, HolyLoisClientTests::shoalIdentity);
            run(failed, "fish variant models", context, world, HolyLoisClientTests::fishVariantModels);
            run(failed, "quiet favorites and first descriptions", context, world, HolyLoisClientTests::uiRefinements);
            run(failed, "crate rarity presentation", context, world, HolyLoisClientTests::cratePresentation);
            run(failed, "fishing history counts", context, world, HolyLoisClientTests::fishingHistory);
            run(failed, "fish tooltips", context, world, HolyLoisClientTests::fishTooltips);
            run(failed, "low-tier meals and full-name shine", context, world, HolyLoisClientTests::lowTierPolish);
            run(failed, "fish pickup and tooltip sections", context, world, HolyLoisClientTests::fishPickupSections);
            run(failed, "fish of thieves eating frames", context, world, HolyLoisClientTests::fishEatingFrames);
            run(failed, "armor 3d", context, world, HolyLoisClientTests::armor3d);
            run(failed, "r over a fillet", context, world, HolyLoisClientTests::rOverFillet);
            run(failed, "offhand swap spam", context, world, HolyLoisClientTests::offhandSwapSpam);
            run(failed, "structure title", context, world, HolyLoisClientTests::structureTitle);
            run(failed, "panorama capture", context, world, HolyLoisClientTests::panoramaCapture);
        } catch (Throwable setup) {
            failed.add("setup: " + setup);
            org.slf4j.LoggerFactory.getLogger("HolyLoisTest").error("Test world setup failed", setup);
        } finally {
            log(failed.isEmpty() ? "ALL HOLY LOIS CLIENT TESTS PASSED" : "HOLY LOIS CLIENT TESTS FAILED: " + failed);
            org.apache.logging.log4j.LogManager.shutdown();
            Runtime.getRuntime().halt(failed.isEmpty() ? 0 : 1);
        }
    }

    private static void run(List<String> failed, String name, ClientGameTestContext context, TestSingleplayerContext world, Test test) {
        // HOLYLOIS_TEST_ONLY=word (or comma-separated words) runs only matching tests.
        String only = System.getenv("HOLYLOIS_TEST_ONLY");
        if (only != null && !only.isBlank() && java.util.Arrays.stream(only.split(",")).map(String::strip).filter(s->!s.isEmpty()).noneMatch(name::contains)) return;
        try {
            test.run(context, world);
            log("TEST OK: " + name);
        } catch (Throwable failure) {
            failed.add(name);
            org.slf4j.LoggerFactory.getLogger("HolyLoisTest").error("TEST FAILED: " + name, failure);
            context.runOnClient(client -> { if (client.player != null) client.player.closeContainer(); });
        }
    }

    /** Liteminer's status lines stay, its operator-only "/amber doctor" hint is gone for everyone. */
    private static void liteminerHint(ClientGameTestContext context, TestSingleplayerContext world) {
        context.runOnClient(client -> {
            check(net.minecraft.client.resources.language.I18n.get("liteminer.connection.doctor_hint").isEmpty(), "Liteminer doctor hint is blank");
            check(net.minecraft.client.resources.language.I18n.get("liteminer.connection.unconfirmed").startsWith("The server"), "Liteminer status text stays");
        });
    }

    private static void inventoryOpens(ClientGameTestContext context, TestSingleplayerContext world) {
        context.getInput().pressKey(options -> options.keyInventory);
        context.waitForScreen(InventoryScreen.class);
        context.takeScreenshot("03-inventory");
        context.runOnClient(client -> client.player.closeContainer());
        context.waitTicks(5);
    }

    /** Test-only journal peer exercises the real request/response codecs and client receiver, without owner data. */
    private static void journalFixture(){
        var queue=new holylois.boombox.JournalRequestQueue();
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(holylois.boombox.JournalRequest.TYPE,(request,context)->{
            if(queue.submit(context.player().getUUID(),request,System.currentTimeMillis()))journalReply(context.player(),request);
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var entry:queue.drain(System.currentTimeMillis()).entrySet()){
                var player=server.getPlayerList().getPlayer(entry.getKey());if(player!=null)journalReply(player,entry.getValue());
            }
        });
    }
    private static void journalReply(net.minecraft.server.level.ServerPlayer player,holylois.boombox.JournalRequest request){
        var all=journalEntries(request.tab());
        var entries=all.stream().filter(e->request.filter()==4?e.first():request.filter()==0||e.kind()==request.filter()).filter(e->e.name().toLowerCase(java.util.Locale.ROOT).contains(request.search().toLowerCase(java.util.Locale.ROOT))).toList();
        int pages=Math.max(1,(entries.size()+request.size()-1)/request.size()),page=Math.min(request.page(),pages-1),start=page*request.size();
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,new holylois.boombox.JournalState(request.tab(),request.filter(),page,pages,entries.size(),request.size(),request.search(),entries.subList(start,Math.min(entries.size(),start+request.size())),request.requestId()));
    }
    private static List<holylois.boombox.JournalState.Entry> journalEntries(int tab){
        var entries=new ArrayList<holylois.boombox.JournalState.Entry>();
        if(tab==1){
            entries.add(new holylois.boombox.JournalState.Entry("fish:cod",1,"minecraft:cod","","minecraft:overworld",150,64,-300,1791670000,12,8.5,63,0,true,false,java.util.List.of(2L,1L,2L,3L,2L,2L),2));
            entries.add(new holylois.boombox.JournalState.Entry("kill:zombie",3,"minecraft:zombie","","minecraft:overworld",150,64,-300,1791660000,48,0,0,75,true,false));
        }
        for(int i=0;i<19;i++)entries.add(new holylois.boombox.JournalState.Entry("place:"+i,2,"Ice Spire "+(i+1),"Explorer","minecraft:overworld",1700+i*300,90,-1700,1791600000-i*3600,1,0,0,0,true,tab==0||i==0));
        return entries;
    }
    private static void journal(ClientGameTestContext context,TestSingleplayerContext world){
        // A preceding auth test closes through the screenshot fade, which is asynchronous.
        context.waitFor(client->client.gui.screen()==null,100);
        context.runOnClient(client->{
            var key=net.minecraft.client.KeyMapping.get("key.holylois.journal");
            var saved=net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(key);
            check(key.getDefaultKey().equals(com.mojang.blaze3d.platform.InputConstants.UNKNOWN),"journal default uses SDL's safe unbound key");
            key.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD.getOrCreate(-1));
            check(holylois.boombox.JournalClient.repairBinding(key)&&key.isUnbound(),"invalid saved journal binding migrates to unbound");
            net.minecraft.client.KeyMapping.setAll();
            check(!holylois.boombox.JournalClient.repairBinding(key),"valid unbound journal preference stays unchanged");
            key.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD.getOrCreate(com.mojang.blaze3d.platform.InputConstants.KEY_K));
            check(!holylois.boombox.JournalClient.repairBinding(key),"valid journal binding stays unchanged");
            key.setKey(saved);
            net.minecraft.client.KeyMapping.resetMapping();
        });
        context.runOnClient(client->{
            var key=net.minecraft.client.KeyMapping.get("key.holylois.journal");
            if(key.isUnbound())key.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD.getOrCreate(com.mojang.blaze3d.platform.InputConstants.KEY_K));
            net.minecraft.client.KeyMapping.resetMapping();
        });
        context.getInput().pressKey(options->net.minecraft.client.KeyMapping.get("key.holylois.journal"));
        context.waitForScreen(holylois.boombox.JournalScreen.class);
        context.waitTicks(12);
        context.takeScreenshot("52-journal-personal");
        context.runOnClient(client->{
            var screen=(holylois.boombox.JournalScreen)client.gui.screen();
            var state=(holylois.boombox.JournalState)field(screen,"state");
            check(state!=null&&state.tab()==1&&state.total()==21,"journal key opens a server-owned personal collection");
            screen.update(new holylois.boombox.JournalState(0,0,0,1,0,state.size(),"old result",java.util.List.of(),Integer.MAX_VALUE));
            check(field(screen,"state")==state,"an unrelated delayed page cannot replace the current filter result");
        });
        context.clickScreenButton("holylois.journal.world");context.waitTicks(12);
        context.takeScreenshot("52-journal-world-firsts");
        context.clickScreenButton(">");context.waitTicks(12);
        context.runOnClient(client->{
            var state=(holylois.boombox.JournalState)field(client.gui.screen(),"state");
            check(state.tab()==0&&state.page()==1&&state.entries().size()<=8,"world firsts page through bounded server responses");
        });
        context.clickScreenButton("holylois.journal.mine");context.waitTicks(12);
        context.clickScreenButton("holylois.journal.filter4");context.waitTicks(12);
        context.runOnClient(client->{var state=(holylois.boombox.JournalState)field(client.gui.screen(),"state");check(state.total()==1&&state.entries().getFirst().first(),"personal firsts are a distinct credited collection");});
        context.takeScreenshot("52-journal-personal-firsts");
        context.clickScreenButton("holylois.journal.filter1");context.waitTicks(12);
        context.runOnClient(client->{
            var screen=(holylois.boombox.JournalScreen)client.gui.screen();
            var state=(holylois.boombox.JournalState)field(screen,"state");
            check(state.total()==1&&state.entries().getFirst().weight()==8.5,"fish category retains the measured best weight");
            check(state.entries().getFirst().shiny()==2&&state.entries().getFirst().rarityCounts().get(4)==2,"fish detail receives actual rarity and shiny totals");
            String fishName=holylois.boombox.JournalScreen.name(state.entries().getFirst()).getString();
            for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.Button button&&button.getMessage().getString().equals(fishName)){button.onPress(new net.minecraft.client.input.KeyEvent(40,0,0));break;}
            check((boolean)field(screen,"detail"),"selecting a journal row opens its details");
        });
        context.waitTicks(5);context.takeScreenshot("52-journal-fish-details");
        context.runOnClient(client->{
            var screen=(holylois.boombox.JournalScreen)client.gui.screen();
            screen.mouseScrolled(screen.width/2.0,screen.height/2.0,0,-4);
            check((int)field(screen,"detailScroll")>0,"long fish history can scroll without covering action buttons");
        });
        context.waitTicks(2);context.takeScreenshot("52-journal-fish-history-scrolled");
        int before=context.computeOnClient(client->waypointCount());
        context.clickScreenButton("holylois.journal.waypoint");context.waitTicks(8);
        check(context.computeOnClient(client->waypointCount())==before+1,"selected journal entry adds a client-only waypoint");
        context.clickScreenButton("holylois.journal.waypoint");context.waitTicks(4);
        check(context.computeOnClient(client->waypointCount())==before+1,"adding the same journal waypoint does not duplicate it");
        context.runOnClient(client->{
            var screen=(holylois.boombox.JournalScreen)client.gui.screen();screen.resize(360,230);
            for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)
                check(widget.getX()>=0&&widget.getY()>=0&&widget.getX()+widget.getWidth()<=360&&widget.getY()+widget.getHeight()<=230,"journal controls fit a small screen");
        });
        context.waitTicks(8);context.takeScreenshot("52-journal-small");
        var closedState=context.computeOnClient(client->(holylois.boombox.JournalState)field(client.gui.screen(),"state"));
        context.runOnClient(client->client.gui.setScreen(null));
        check(context.computeOnClient(client->client.gui.screen()==null),"journal closes without trapping the cursor");
        world.getServer().runOnServer(server->net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(server.getPlayerList().getPlayers().getFirst(),closedState));
        context.waitTicks(8);
        check(context.computeOnClient(client->client.gui.screen()==null),"a late network reply cannot reopen a closed journal");
        world.getServer().runOnServer(server->net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(server.getPlayerList().getPlayers().getFirst(),new holylois.boombox.JournalState(
            closedState.tab(),closedState.filter(),closedState.page(),closedState.pages(),closedState.total(),closedState.size(),closedState.search(),closedState.entries())));
        context.waitForScreen(holylois.boombox.JournalScreen.class);context.waitTicks(8);
        check(context.computeOnClient(client->client.gui.screen() instanceof holylois.boombox.JournalScreen),"explicit server command pages still open the journal");
        context.runOnClient(client->{
            var screen=(holylois.boombox.JournalScreen)client.gui.screen();
            int size=(int)field(screen,"rows");
            screen.update(new holylois.boombox.JournalState(1,1,0,1,0,size,"",java.util.List.of()));
        });
        context.takeScreenshot("52-journal-empty-fish");
        context.runOnClient(client->{
            var screen=new holylois.boombox.JournalScreen(1);client.gui.setScreen(screen);
            try{var requested=screen.getClass().getDeclaredField("requestedAt");requested.setAccessible(true);requested.setLong(screen,System.currentTimeMillis()-6000);}
            catch(ReflectiveOperationException error){throw new AssertionError(error);}
            screen.tick();
            check((boolean)field(screen,"timedOut")&&!(boolean)field(screen,"loading"),"unanswered journal requests offer Retry instead of an endless spinner");
        });
        context.takeScreenshot("52-journal-retry");
        context.clickScreenButton("holylois.journal.retry");context.waitTicks(20);
        context.runOnClient(client->{
            check(field(client.gui.screen(),"state")!=null,"Retry receives a real bounded page through the throttled peer");
            client.gui.setScreen(null);holylois.boombox.JournalHints.added();holylois.boombox.JournalHints.added();
        });
        context.waitTicks(5);context.takeScreenshot("52-journal-hint");
    }
    private static int waypointCount(){
        var session=BuiltInHudModules.MINIMAP.getCurrentSession();var root=session==null?null:session.getWorldManager().getCurrentRootContainer();
        int count=0;if(root!=null)for(var w:root.getAllWorldsIterable())for(var set:w.getIterableWaypointSets())for(var point:set.getWaypoints())count++;return count;
    }
    private static void cinematicCards(ClientGameTestContext context,TestSingleplayerContext world){
        context.runOnClient(client->{client.gui.setScreen(null);ji("cinematic.CinematicManager","toggleImmediate");});
        context.waitFor(client->holylois.boombox.CinematicTweaks.playing(),200);
        context.runOnClient(client->ji("render.HUDController","setHidden",true));
        check(context.computeOnClient(client->(boolean)ji("render.HUDController","isHidden")),"the real cinematic HUD cancellation is active");
        int before=context.computeOnClient(client->holylois.boombox.RareCatchHud.cardsDrawn);
        world.getServer().runOnServer(server->net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(server.getPlayerList().getPlayers().getFirst(),new holylois.boombox.RareCatchNotice("minecraft:cod","legendary",22,true)));
        context.waitTicks(15);context.takeScreenshot("53-catch-card-over-cinematic");
        check(context.computeOnClient(client->holylois.boombox.RareCatchHud.cardsDrawn)>before,"catch cards remain visible when the cinematic skips normal HUD extraction");
        context.runOnClient(client->holylois.boombox.JournalHints.added());context.waitTicks(3);
        check(context.computeOnClient(client->(long)field(holylois.boombox.JournalHints.class,"started"))==0,"a Journal notice waits for the cinematic to end instead of expiring unseen");
        context.runOnClient(client->{try{var setting=holylois.boombox.CinematicCards.class.getDeclaredField("enabled");setting.setAccessible(true);setting.setBoolean(null,false);}catch(Exception error){throw new AssertionError(error);}});
        int muted=context.computeOnClient(client->holylois.boombox.RareCatchHud.cardsDrawn);
        context.waitTicks(6);
        check(context.computeOnClient(client->holylois.boombox.RareCatchHud.cardsDrawn)==muted,"cinematic card off leaves the camera view clear");
        context.runOnClient(client->{try{var setting=holylois.boombox.CinematicCards.class.getDeclaredField("enabled");setting.setAccessible(true);setting.setBoolean(null,true);}catch(Exception error){throw new AssertionError(error);}ji("cinematic.CinematicManager","forceDeactivate");});
        context.waitTicks(4);
    }

    /**
     * A boombox placed into flowing water must not become a water source (it only waterlogs in a real source), and
     * breaking either kind must leave the same number of sources as before. Goes through BlockItem.place like a player.
     */
    private static void boomboxKeepsWaterSources(ClientGameTestContext context, TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("fill 19 -62 19 27 -61 21 stone");
        server.runCommand("fill 20 -61 20 26 -61 20 air");
        server.runCommand("setblock 20 -61 20 water");
        context.waitTicks(80);
        int before = server.computeOnServer(s -> sources(s.overworld()));
        check(before == 1, "trench has exactly one water source before (" + before + ")");
        BlockPos flowing = new BlockPos(23, -61, 20), source = new BlockPos(20, -61, 20);
        server.runOnServer(s -> {
            check(!s.overworld().getFluidState(flowing).isSource() && !s.overworld().getFluidState(flowing).isEmpty(), "target cell holds flowing water");
            place(s, flowing);
            place(s, source);
            check(!s.overworld().getBlockState(flowing).getValue(BlockStateProperties.WATERLOGGED), "boombox in flowing water is not waterlogged");
            check(s.overworld().getBlockState(source).getValue(BlockStateProperties.WATERLOGGED), "boombox in a water source is waterlogged");
        });
        context.waitTicks(80);
        int placed = server.computeOnServer(s -> sources(s.overworld()));
        check(placed == 1, "no extra water source while boomboxes sit in water (" + placed + ")");
        server.runOnServer(s -> { s.overworld().destroyBlock(flowing, false); s.overworld().destroyBlock(source, false); });
        context.waitTicks(80);
        int after = server.computeOnServer(s -> sources(s.overworld()));
        check(after == 1, "breaking the boomboxes leaves exactly the original water source (" + after + ")");
    }

    private static void place(net.minecraft.server.MinecraftServer server, BlockPos target) {
        var player = server.getPlayerList().getPlayers().getFirst();
        var item = (BlockItem) BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("holylois", "boombox"));
        BlockPos below = target.below();
        var hit = new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false);
        var result = item.place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(item), hit));
        check(result.consumesAction() && server.overworld().getBlockState(target).is(item.getBlock()), "boombox placed at " + target.toShortString());
    }

    private static int sources(ServerLevel level) {
        int count = 0;
        for (int x = 19; x <= 27; x++) for (int z = 19; z <= 21; z++) for (int y = -62; y <= -60; y++)
            if (level.getFluidState(new BlockPos(x, y, z)).isSource()) count++;
        return count;
    }

    /**
     * Reported: a closed chest replays its closing animation after you look away and back. Opens and closes a chest
     * like a player, turns around, turns back and watches the lid tick by tick (plus screenshots for render-only cases).
     */
    private static void chestLidStaysShut(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        BlockPos base = context.computeOnClient(client -> client.player.blockPosition());
        BlockPos chest = base.offset(3, 0, 0);
        world.getServer().runCommand("setblock " + chest.getX() + " " + chest.getY() + " " + chest.getZ() + " chest[facing=west]");
        context.waitTicks(20);
        context.getInput().lookAt(chest);
        context.waitTicks(5);
        context.getInput().pressKey(options -> options.keyUse);
        context.waitForScreen(ContainerScreen.class);
        context.waitTicks(20);
        context.runOnClient(client -> client.player.closeContainer());
        context.waitTicks(60);
        float settled = context.computeOnClient(client -> lid(client, chest));
        check(settled == 0f, "lid closed after the chest was closed (" + settled + ")");
        context.takeScreenshot("06-chest-closed");
        context.getInput().lookAt(base.offset(-3, 0, 0));
        context.waitTicks(60);
        context.getInput().lookAt(chest);
        float most = 0;
        for (int tick = 0; tick < 30; tick++) {
            context.waitTick();
            most = Math.max(most, context.computeOnClient(client -> lid(client, chest)));
            if (tick == 2 || tick == 6) context.takeScreenshot("07-chest-after-look-back-" + tick);
        }
        check(most == 0f, "lid does not move again after looking away and back (max openness " + most + ")");
        // Players usually turn away right after closing, while the lid is still moving: check that on screen too.
        double quick = lookBackMovement(context, chest, base, 3, "quick");
        double later = lookBackMovement(context, chest, base, 80, "later");
        log("chest lid movement on screen after looking back: turned away right after closing " + quick + ", long after " + later);
        check(quick < 2.0 && later < 2.0, "lid does not visibly move after looking back (" + quick + ", " + later + ")");
        // Owner 2026-10-10: open the chest, get teleported far away with it open, come back: the lid must be shut.
        var player = context.computeOnClient(client -> client.player.getName().getString());
        context.getInput().lookAt(chest);
        context.waitTicks(5);
        context.getInput().pressKey(options -> options.keyUse);
        context.waitForScreen(ContainerScreen.class);
        context.waitTicks(20);
        world.getServer().runCommand("tp " + player + " " + (base.getX() + 100) + " " + base.getY() + " " + base.getZ());
        context.waitTicks(80);
        world.getServer().runCommand("tp " + player + " " + base.getX() + " " + base.getY() + " " + base.getZ());
        context.waitTicks(60);
        context.getInput().lookAt(chest);
        context.waitTicks(40);
        float afterTeleport = context.computeOnClient(client -> lid(client, chest));
        context.takeScreenshot("09b-chest-after-teleport-back");
        log("chest lid after a teleport with the chest open: " + afterTeleport);
        check(afterTeleport == 0f, "the lid is shut when you come back after teleporting away with the chest open (" + afterTeleport + ")");
    }

    /** Opens and closes the chest, turns away after delay ticks, turns back; returns screen change at the chest between look-back and settled. */
    private static double lookBackMovement(ClientGameTestContext context, BlockPos chest, BlockPos base, int delay, String name) throws Exception {
        context.getInput().lookAt(chest);
        context.waitTicks(5);
        context.getInput().pressKey(options -> options.keyUse);
        context.waitForScreen(ContainerScreen.class);
        context.waitTicks(20);
        context.runOnClient(client -> client.player.closeContainer());
        context.waitTicks(delay);
        context.getInput().lookAt(base.offset(-(chest.getX() - base.getX()), 0, -(chest.getZ() - base.getZ())));
        context.waitTicks(60);
        context.getInput().lookAt(chest);
        context.waitTicks(1);
        var early = javax.imageio.ImageIO.read(context.takeScreenshot("08-fa-chest-" + name + "-back").toFile());
        context.waitTicks(40);
        var settled = javax.imageio.ImageIO.read(context.takeScreenshot("09-fa-chest-" + name + "-settled").toFile());
        return centreDifference(early, settled);
    }

    /** Mean per-channel difference over the middle fifth of the screen (where the chest is when looked at). */
    static double centreDifference(java.awt.image.BufferedImage a, java.awt.image.BufferedImage b) {
        int w = a.getWidth(), h = a.getHeight(), x0 = w * 2 / 5, y0 = h * 2 / 5, x1 = w * 3 / 5, y1 = h * 3 / 5;
        long total = 0, count = 0;
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
            int p = a.getRGB(x, y), q = b.getRGB(x, y);
            for (int shift = 0; shift <= 16; shift += 8) total += Math.abs((p >> shift & 255) - (q >> shift & 255));
            count += 3;
        }
        return Math.round(total * 100.0 / count) / 100.0;
    }

    private static float lid(net.minecraft.client.Minecraft client, BlockPos pos) {
        return client.level.getBlockEntity(pos) instanceof ChestBlockEntity chest ? chest.getOpenNess(1f) : -1f;
    }

    /** Client half of the death marker cleanup: Xaero makes a death marker, the server says the loot is gone, it disappears. */
    private static void deathMarkerGoesWhenLootIsGone(ClientGameTestContext context, TestSingleplayerContext world) {
        // Die far from spawn: Xaero deletes a death marker you stand on (delete_reached_deathpoints), and respawn is at spawn.
        world.getServer().runCommand("tp @a 200 -60 200");
        world.getConnection().waitForChunksRender();
        world.getServer().runCommand("kill @a");
        context.waitForScreen(DeathScreen.class);
        context.waitTicks(40);
        context.takeScreenshot("04-death-screen");
        context.clickScreenButton("deathScreen.respawn");
        context.waitFor(client -> client.player != null && client.player.isAlive(), 600);
        world.getConnection().waitForChunksRender();
        context.waitTicks(120); // the client hands the packet to Xaero after its session settles
        int before = context.computeOnClient(client -> deathMarkers());
        check(before >= 1, "Xaero made a death marker (found " + before + ")");
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            var death = player.getLastDeathLocation().orElseThrow();
            check(DeathLootGone.send(player, death.dimension().identifier(), death.pos()), "server could send death_loot_gone");
        });
        context.waitTicks(60);
        int after = context.computeOnClient(client -> deathMarkers());
        check(after == before - 1, "death marker removed after the loot was gone (" + before + " -> " + after + ")");
        context.takeScreenshot("05-after-loot-gone");
    }

    /** Test catches (/legends give): rarity, weight past the cod maximum for a Mythic, then how they look in the slots, hand and on the ground. */
    private static void trophyFishLook(ClientGameTestContext context, TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("clear @a");
        for (String rarity : new String[]{"mythic", "legendary", "epic", "rare"}) server.runCommand("execute as @p run legends give " + rarity);
        context.waitTicks(10);
        server.runOnServer(s -> {
            var player = s.getPlayerList().getPlayers().getFirst();
            var first = player.getInventory().getItem(0);
            var data = first.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            check(data != null, "the test fish has custom data");
            var fish = data.copyTag().getCompound("holylois_fish").orElseThrow();
            check(fish.getStringOr("rarity", "").equals("mythic"), "first test fish is Mythic (" + fish + ")");
            check(fish.getDoubleOr("kg", 0) >= 18 && fish.getDoubleOr("size", 0) >= 1.5, "a Mythic cod weighs past 1.5x the 12 kg maximum (" + fish + ")");
            check(first.get(net.minecraft.core.component.DataComponents.CONSUMABLE).onConsumeEffects().size() >= 1, "a Mythic fish has eating buffs");
            player.getInventory().setSelectedSlot(0);
        });
        context.waitTicks(20);
        context.takeScreenshot("10-fish-hotbar-and-hand");
        float scale = context.computeOnClient(client -> holylois.boombox.FishLook.scale(client.player.getMainHandItem(), net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND));
        float own = context.computeOnClient(client -> holylois.boombox.FishLook.scale(client.player.getMainHandItem(), net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND));
        check(scale > 2 && own > 1.2f && own < scale - 0.4f, "a Mythic fish is drawn much bigger in the hand, less in your own view (" + scale + ", " + own + ")");
        context.getInput().pressKey(options -> options.keyInventory);
        context.waitForScreen(InventoryScreen.class);
        context.waitTicks(5);
        context.takeScreenshot("11-fish-inventory");
        context.runOnClient(client -> client.player.closeContainer());
        context.waitTicks(5);
        BlockPos spot = context.computeOnClient(client -> client.player.blockPosition().offset(-1, 0, 3));
        server.runOnServer(s -> {
            var player = s.getPlayerList().getPlayers().getFirst();
            var plain = new net.minecraft.world.entity.item.ItemEntity(s.overworld(), spot.getX() + 1.5, spot.getY() + 0.2, spot.getZ() + 0.5, new ItemStack(net.minecraft.world.item.Items.COD));
            plain.setDeltaMovement(Vec3.ZERO); plain.setPickUpDelay(32767); s.overworld().addFreshEntity(plain);
            for (int slot = 0; slot < 2; slot++) {
                var item = new net.minecraft.world.entity.item.ItemEntity(s.overworld(), spot.getX() + 0.5, spot.getY() + 0.2, spot.getZ() + 0.5 + slot, player.getInventory().removeItemNoUpdate(slot));
                item.setDeltaMovement(Vec3.ZERO); item.setPickUpDelay(32767);
                s.overworld().addFreshEntity(item);
            }
        });
        context.getInput().lookAt(spot);
        context.waitTicks(120); // the rare catch card from /legends give fades after 5 s
        log("ground fish scale " + context.computeOnClient(client -> {
            var sb = new StringBuilder();
            for (var e : client.level.entitiesForRendering()) if (e instanceof net.minecraft.world.entity.item.ItemEntity item)
                sb.append(item.position()).append(" ").append(holylois.boombox.FishLook.scale(item.getItem(), net.minecraft.world.item.ItemDisplayContext.GROUND)).append("; ");
            return sb.toString();
        }));
        context.takeScreenshot("12-fish-on-ground");
        server.runCommand("kill @e[type=item]");
    }

    /** The daily card (centred above the hotbar, star pop, item pop, fade) and every Holy Lois menu, for the look and spacing. */
    @SuppressWarnings("unchecked")
    private static void menusAndDailyCard(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        context.runOnClient(client -> {
            try {
                var field = Class.forName("holylois.auth.RewardHud").getDeclaredField("queue"); field.setAccessible(true);
                ((java.util.Deque<Object>) field.get(null)).addLast(new holylois.auth.RewardNotice("minecraft:bread", 6, 3, 100, 0));
            } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        });
        context.waitTicks(4);
        fakeParty(context);
        context.takeScreenshot("20-daily-card-fading-in");
        context.waitTicks(16);
        fakeParty(context);
        context.takeScreenshot("21-daily-card-star");
        context.waitTicks(40);
        fakeParty(context);
        context.takeScreenshot("22-daily-card");
        context.runOnClient(client -> {
            try {
                var field = Class.forName("holylois.auth.RewardHud").getDeclaredField("queue"); field.setAccessible(true);
                ((java.util.Deque<Object>) field.get(null)).addLast(new holylois.auth.RewardNotice("holylois:holy_lootbox", 1, 7, 0, 0));
            } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        });
        context.waitTicks(150);
        context.takeScreenshot("22-daily-card-day7");
        // Half way through the fade-out: the lootbox shrinks with the card instead of popping away.
        context.waitTicks(93);
        context.takeScreenshot("22-daily-card-fading-out-1");
        context.waitTicks(4);
        context.takeScreenshot("22-daily-card-fading-out-2");
        context.waitTicks(4);
        context.takeScreenshot("22-daily-card-fading-out-3");
        context.waitTicks(69);
        var homes = new holylois.boombox.HomeState(true, 3, 1, List.of(new holylois.boombox.HomeState.Home("Base", "minecraft:overworld")));
        context.runOnClient(client -> client.gui.setScreen(new holylois.boombox.HomesScreen(homes)));
        context.waitTicks(5);
        context.takeScreenshot("23-homes");
        // Delete confirmation: only the destructive button is red, Cancel stays neutral (owner 2026-10-10).
        context.runOnClient(client -> {
            try {
                var screen = client.gui.screen();
                var type = holylois.boombox.HomesScreen.class;
                var edit = type.getDeclaredField("edit"); edit.setAccessible(true); edit.setInt(screen, holylois.boombox.TravelIntent.DELETE);
                var old = type.getDeclaredField("old"); old.setAccessible(true); old.set(screen, "Base");
                var confirm = type.getDeclaredMethod("confirmDelete"); confirm.setAccessible(true); confirm.invoke(screen);
            } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        });
        context.waitTicks(5);
        context.takeScreenshot("23-homes-delete-confirm");
        context.runOnClient(client -> client.gui.setScreen(new holylois.boombox.PartyScreen()));
        context.waitTicks(5);
        context.takeScreenshot("24-party");
        context.runOnClient(client -> client.gui.setScreen(new holylois.boombox.RallyScreen()));
        context.waitTicks(5);
        context.takeScreenshot("25-rally");
        context.runOnClient(client -> client.gui.setScreen(new holylois.boombox.AccountScreen(new holylois.boombox.AccountNotice(2, java.util.UUID.randomUUID(), 6, false, true, ""))));
        context.waitTicks(5);
        context.takeScreenshot("26-account-password");
        context.runOnClient(client -> client.gui.setScreen(new holylois.auth.HolyLoisAuthScreen(new holylois.auth.AuthStatus(2, 6))));
        context.waitTicks(5);
        context.takeScreenshot("27-register");
        boolean hudHidden = context.computeOnClient(client -> client.gui.hud.isHidden());
        // Signed in: the form's last frame fades out over the world instead of cutting to it.
        context.runOnClient(client -> client.gui.setScreen(null));
        context.waitTicks(4);
        boolean fading = context.computeOnClient(client -> holylois.auth.PanoramaFade.fading() && client.gui.screen() == null);
        context.takeScreenshot("27-entry-fade");
        context.waitTicks(30);
        boolean done = context.computeOnClient(client -> !holylois.auth.PanoramaFade.fading());
        check(fading && done, "the login backdrop fades into the world (" + fading + ", " + done + ")");
        boolean hudBack = context.computeOnClient(client -> !client.gui.hud.isHidden());
        check(hudHidden && hudBack, "the HUD hides behind the login panorama and comes back after (" + hudHidden + ", " + hudBack + ")");
        // The resource pack download on the loading bar, text under it, no toast.
        holylois.boombox.PackLoadingBar.downloadStart(java.util.OptionalLong.of(1000));
        holylois.boombox.PackLoadingBar.downloaded(450);
        context.waitTicks(2);
        context.takeScreenshot("28-pack-download");
        holylois.boombox.PackLoadingBar.finished(true);
        context.waitTicks(2);
        context.takeScreenshot("28-pack-applying");
        context.waitTicks(40);
        context.runOnClient(client -> client.gui.setScreen(null));
    }

    /** A party of four (one with absorption, one in another dimension, one offline) and a rally, fresh for the next 3 s. */
    private static void fakeParty(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var me = client.player.getUUID();
            var members = List.of(
                new holylois.boombox.PartyState.Member(me, client.player.getGameProfile().name(), true, true, 20, 20, 0, 20, true),
                new holylois.boombox.PartyState.Member(java.util.UUID.randomUUID(), "Marek", true, true, 13, 20, 6, 18, false),
                new holylois.boombox.PartyState.Member(java.util.UUID.randomUUID(), "LongNameExplorer", true, false, 6, 24, 0, 9, false),
                new holylois.boombox.PartyState.Member(java.util.UUID.randomUUID(), "Sleepy", false, false, 0, 20, 0, 0, false));
            var rally = new holylois.boombox.PartyState.Rally(members.get(1).id(), "Marek", client.level.dimension().identifier().toString(), 40, -60, 25, 75);
            try {
                var hud = holylois.boombox.PartyHud.class;
                var state = hud.getDeclaredField("state"); state.setAccessible(true); state.set(null, new holylois.boombox.PartyState(java.util.UUID.randomUUID(), members, rally));
                var at = hud.getDeclaredField("receivedAt"); at.setAccessible(true); at.setLong(null, System.currentTimeMillis());
            } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        });
    }

    /** A Shiny Legendary (20-40 kg): catch card with sweep and sparkles, shimmering chat, carried in both arms (you and a mannequin). */
    private static void fishCarryShineCard(ClientGameTestContext context, TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("clear @a");
        server.runCommand("kill @e[type=mannequin]");
        server.runCommand("execute as @p run legends give shiny");
        context.waitTicks(8);
        context.takeScreenshot("30-catch-card-sweep");
        context.waitTicks(14);
        context.takeScreenshot("31-catch-card");
        server.runOnServer(s -> {
            var player = s.getPlayerList().getPlayers().getFirst();
            var fish = player.getInventory().getItem(0);
            check(holylois.boombox.FishData.twoHanded(fish), "a Legendary is carried in both arms (" + fish.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA) + ")");
            player.getInventory().setSelectedSlot(0);
        });
        context.waitTicks(100); // the catch card fades after 5.5 s
        context.runOnClient(client -> { client.player.setXRot(0); client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); });
        context.waitTicks(15);
        context.takeScreenshot("32-fish-two-handed-front");
        context.getInput().holdKey(options -> options.keyUp);
        context.waitTicks(8);
        context.takeScreenshot("32-fish-walk");
        context.getInput().releaseKey(options -> options.keyUp);
        context.getInput().pressKey(options -> options.keyJump);
        context.waitTicks(4);
        context.takeScreenshot("32-fish-jump");
        context.waitTicks(15);
        // Your own view: the fists sit beside the head, so nothing clips the camera; looking up shows the fish overhead.
        context.runOnClient(client -> { client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); client.player.setXRot(0); });
        context.waitTicks(5);
        context.takeScreenshot("34-fish-first-person");
        context.runOnClient(client -> client.player.setXRot(-70));
        context.waitTicks(5);
        context.takeScreenshot("34-fish-first-person-up");
        // Both hands: a torch in the off hand goes back to the inventory while the fish is held, and the fish cannot go there.
        server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(net.minecraft.world.item.Items.TORCH)));
        context.waitTicks(8);
        server.runOnServer(s -> {
            var player = s.getPlayerList().getPlayers().getFirst();
            check(player.getOffhandItem().isEmpty() && player.getInventory().countItem(net.minecraft.world.item.Items.TORCH) == 1, "the off hand is cleared while a heavy fish is held");
            var fish = player.getMainHandItem().copy();
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, fish);
        });
        context.waitTicks(8);
        server.runOnServer(s -> {
            var player = s.getPlayerList().getPlayers().getFirst();
            check(player.getOffhandItem().isEmpty(), "a heavy fish does not stay in the off hand");
            int slot = -1;
            for (int i = 0; i < 36; i++) if (holylois.boombox.FishData.twoHanded(player.getInventory().getItem(i))) slot = i;
            check(slot >= 0, "the heavy fish went back into the inventory");
            player.getInventory().setItem(0, player.getInventory().removeItemNoUpdate(slot));
            player.getInventory().setSelectedSlot(0);
        });
        context.runOnClient(client -> { client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); client.player.setXRot(0); });
        context.waitTicks(5);
        // Two still mannequins 3 blocks ahead: the left one faces the camera, the right one shows its side.
        var look = context.computeOnClient(client -> client.player.getLookAngle().multiply(1, 0, 1).normalize());
        var base = context.computeOnClient(client -> client.player.position());
        var right = new net.minecraft.world.phys.Vec3(-look.z, 0, look.x);
        float facing = context.computeOnClient(client -> client.player.getYRot() + 180);
        var front = base.add(look.scale(3)).add(right.scale(-1));
        var side = base.add(look.scale(3)).add(right.scale(1));
        server.runCommand(String.format(java.util.Locale.ROOT, "summon mannequin %.2f %.2f %.2f {Tags:[\"hltest\"],Rotation:[%.1ff,0f]}", front.x, front.y, front.z, facing));
        server.runCommand(String.format(java.util.Locale.ROOT, "summon mannequin %.2f %.2f %.2f {Tags:[\"hltest\"],Rotation:[%.1ff,0f]}", side.x, side.y, side.z, facing - 90));
        server.runOnServer(s -> {
            var fish = s.getPlayerList().getPlayers().getFirst().getInventory().getItem(0);
            for (var e : s.overworld().getAllEntities()) if (e.entityTags().contains("hltest") && e instanceof net.minecraft.world.entity.LivingEntity living)
                living.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, fish.copy());
        });
        context.runOnClient(client -> { client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); client.player.setXRot(-5); });
        server.runCommand("clear @a");
        context.waitTicks(20);
        context.takeScreenshot("33-fish-two-handed-mannequins");
        server.runCommand("kill @e[type=mannequin]");
    }

    /** A Legendary on the cutting board: several knife cuts, a damage bar after each, slices by weight, equal slices stack. */
    private static void filletCuts(ClientGameTestContext context, TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("clear @a");
        server.runCommand("kill @e[type=item]");
        server.runCommand("execute as @p run legends give legendary");
        context.waitTicks(5);
        server.runOnServer(s -> {
            try {
                var player = s.getPlayerList().getPlayers().getFirst();
                var level = s.overworld();
                var fish = player.getInventory().getItem(0).copy();
                int slices = holylois.boombox.FishData.slices(fish), cuts = holylois.boombox.FishData.cuts(fish);
                check(slices >= 15 && cuts >= 4, "a 20-40 kg Legendary gives 15+ slices over 4+ cuts (" + slices + ", " + cuts + ")");
                BlockPos pos = player.blockPosition().offset(3, 0, 3);
                level.setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.getValue(Identifier.parse("farmersdelight:cutting_board")).defaultBlockState());
                var board = level.getBlockEntity(pos);
                board.getClass().getMethod("addItem", ItemStack.class).invoke(board, fish);
                var knife = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("farmersdelight:iron_knife")));
                var cut = board.getClass().getMethod("processStoredItemUsingTool", ItemStack.class, net.minecraft.world.entity.player.Player.class);
                var stored = board.getClass().getMethod("getStoredItem");
                int used = 0;
                while (used < cuts + 3 && !((ItemStack) stored.invoke(board)).isEmpty()) {
                    cut.invoke(board, knife, player);
                    used++;
                    var left = (ItemStack) stored.invoke(board);
                    if (used == 1) check(!left.isEmpty() && left.getDamageValue() == 1 && left.isDamaged(), "after the first cut the fish is still there, one cut damaged (" + left.getDamageValue() + ")");
                }
                check(used == cuts, "the fish is used up on the last cut (" + used + " of " + cuts + ")");
                int dropped = 0; var kinds = new java.util.HashSet<String>();
                for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(3)))
                    if (holylois.boombox.FishData.fillet(item.getItem())) { dropped += item.getItem().getCount(); kinds.add(item.getItem().getComponents().toString()); }
                check(dropped == slices, "slices dropped match the weight (" + dropped + " of " + slices + ")");
                check(kinds.size() == 1, "all slices are the same, so they stack (" + kinds.size() + " kinds)");
                level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        });
        context.waitTicks(5);
        server.runCommand("kill @e[type=item]");
    }

    /**
     * Not Enough Animations with the FirstPerson body and Fresh Animations (KeptArms): a map held up in view, eating at the
     * mouth and a lantern, in first person (body arms, no vanilla hands) and from the front.
     */
    /**
     * Owner report: water looks darker and less clear the closer the camera is to it. A lake over a black and white checker floor
     * is shot from the same spot at five heights, shaders off/on, Distant Horizons off/on, and with Complementary's reflections
     * off or its Reimagined water style (shots 48-water-*). Result 2026-10-11: with DH on, Unbound water reflects DH's dark far
     * terrain at low angles; the Reimagined water style stays light at every height.
     */
    private static void waterByHeight(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        // A diagnostic (it switches shaders and takes minutes): only with HOLYLOIS_TEST_ONLY="water by camera".
        String only = System.getenv("HOLYLOIS_TEST_ONLY");
        if (only == null || !only.contains("water by camera")) { log("water by camera height: diagnostic, skipped"); return; }
        var server = world.getServer();
        for (String c : new String[] {"gamemode spectator @a", "time set noon", "weather clear", "gamerule advance_time false",
                                      "fill 0 -63 0 63 -63 63 white_concrete", "fill 0 -62 0 63 -56 63 water"}) server.runCommand(c);
        for (int tx = 0; tx < 16; tx++) for (int tz = 0; tz < 16; tz++)
            if ((tx + tz) % 2 == 1) server.runCommand("fill " + tx * 4 + " -63 " + tz * 4 + " " + (tx * 4 + 3) + " -63 " + (tz * 4 + 3) + " black_concrete");
        // The shader list with our names and strain colours (ShaderPackNamesMixin / ShaderPackListMixin).
        context.setScreen(() -> {
            try { return (net.minecraft.client.gui.screens.Screen) Class.forName("net.irisshaders.iris.gui.screen.ShaderPackScreen").getConstructor(net.minecraft.client.gui.screens.Screen.class).newInstance((Object) null); }
            catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        });
        context.waitTicks(20);
        context.takeScreenshot("47-shader-list");
        context.setScreen(() -> null);
        context.getInput().pressKey(options -> options.keyToggleGui);
        double[] heights = {2.5, 3.5, 3.9, 4.1, 4.5, 6, 12};
        for (boolean shaders : new boolean[] {false, true}) {
            context.runOnClient(client -> {
                try {
                    var api = Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance").invoke(null);
                    var config = api.getClass().getMethod("getConfig").invoke(api);
                    config.getClass().getMethod("setShadersEnabledAndApply", boolean.class).invoke(config, shaders);
                } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            });
            context.waitTicks(shaders ? 300 : 40);
            for (String mode : System.getenv("HOLYLOIS_WATER_PILLAR") != null ? new String[] {"dh"} : new String[] {"dh", "nodh"}) {
                // Iris cannot rebuild its pipeline while Distant Horizons renders: set the option first, then turn DH on.
                if (shaders) { dhRendering(context, false); context.waitTicks(10); shaderOption(context, switch (mode) { case "dhnoref" -> "WATER_REFLECT_QUALITY=-1"; case "dhstyle1" -> "WATER_STYLE_DEFINE=1"; default -> ""; }); }
                dhRendering(context, !mode.equals("nodh"));
                context.waitTicks(mode.equals("nodh") ? 40 : 200);
                for (double h : System.getenv("HOLYLOIS_WATER_PILLAR") != null ? new double[] {4.5} : heights) {
                    server.runCommand(String.format(java.util.Locale.ROOT, "tp @p 32 %.2f 4 facing 32 -55 40", -55 + h - 1.62));
                    context.waitTicks(25);
                    context.takeScreenshot(String.format(java.util.Locale.ROOT, "48-water-%s-%s-h%04.1f", shaders ? "shaders" : "plain", mode, h));
                }
            }
            pillarCamera(context, server, shaders ? "shaders" : "plain");
            if (shaders) { dhRendering(context, false); context.waitTicks(10); shaderOption(context, ""); }
        }
        dhRendering(context, true);
        context.runOnClient(client -> {
            try {
                var api = Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance").invoke(null);
                var config = api.getClass().getMethod("getConfig").invoke(api);
                config.getClass().getMethod("setShadersEnabledAndApply", boolean.class).invoke(config, false);
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        });
        context.getInput().pressKey(options -> options.keyToggleGui);
        for (String c : new String[] {"gamemode survival @a", "gamerule advance_time true", "fill 0 -63 0 63 -56 63 air", "fill 0 -63 0 63 -62 63 dirt", "fill 0 -61 0 63 -61 63 grass_block", "tp @p 0 -60 0"}) server.runCommand(c);
        context.waitTicks(20);
    }

    /** Picking up an enchanted sword shows an item card; a plain stick does not; the same sword again within 30 s stays quiet. */
    private static void itemCards(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        var server = world.getServer();
        server.runCommand("gamemode survival @a");
        server.runCommand("clear @a");
        int before = context.computeOnClient(client -> holylois.boombox.ItemCardsTestHook.shown());
        server.runCommand("execute at @p run summon item ~ ~ ~ {Item:{id:\"minecraft:stick\",count:1},PickupDelay:0}");
        context.waitTicks(30);
        check(context.computeOnClient(client -> holylois.boombox.ItemCardsTestHook.shown()) == before, "a plain stick shows no card");
        server.runCommand("execute at @p run summon item ~ ~ ~ {Item:{id:\"minecraft:diamond_sword\",count:1,components:{\"minecraft:enchantments\":{\"minecraft:sharpness\":5,\"minecraft:looting\":3}}},PickupDelay:0}");
        context.waitTicks(12);
        context.takeScreenshot("51-item-card-enchanted");
        int after = context.computeOnClient(client -> holylois.boombox.ItemCardsTestHook.shown());
        check(after == before + 1, "an enchanted sword shows an item card (" + before + " -> " + after + ")");
        server.runCommand("clear @a");
        server.runCommand("execute at @p run summon item ~ ~ ~ {Item:{id:\"minecraft:diamond_sword\",count:1,components:{\"minecraft:enchantments\":{\"minecraft:sharpness\":5}}},PickupDelay:0}");
        context.waitTicks(120);
        check(context.computeOnClient(client -> holylois.boombox.ItemCardsTestHook.shown()) == after, "the same sword again within 30 s shows no second card");
        server.runCommand("clear @a");
    }

    /** A hooked swimming Fish of Thieves fish turns into its item with the variant, a weight roll and no bucket data. */
    private static void schoolCatch(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        var server = world.getServer();
        server.runCommand("clear @a");
        server.runOnServer(s -> {
            var player = s.getPlayerList().getPlayers().getFirst();
            var level = (net.minecraft.server.level.ServerLevel) player.level();
            var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse("fishofthieves:splashtail"));
            var fish = (net.minecraft.world.entity.animal.fish.AbstractFish) type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            fish.setPos(player.getX()+6, player.getY(), player.getZ());
            level.addFreshEntity(fish);
            var item = holylois.boombox.SchoolCatch.itemFor(fish);
            check(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString().equals("fishofthieves:splashtail"), "a hooked Splashtail becomes the Splashtail item (" + item + ")");
            check(!item.has(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA), "no bucket data on the caught fish");
            check(holylois.boombox.SchoolCatch.reel(player, fish) && !fish.isAlive(), "reeling the hooked fish removes the mob");
            var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,player.getBoundingBox().inflate(12));
            var drop=drops.stream().filter(d->BuiltInRegistries.ITEM.getKey(d.getItem().getItem()).toString().equals("fishofthieves:splashtail")).findFirst().orElseThrow();
            check(drop.getDeltaMovement().x<0&&drop.getDeltaMovement().y>0,"caught fish flies from the hook toward the angler");
            check(holylois.boombox.FishData.kilograms(drop.getItem())==holylois.boombox.FishData.kilograms(item),"caught swimming fish keeps its visible weight");
            drop.discard();
        });
        server.runCommand("clear @a");
    }

    /** One rarity system, ours: vanilla, enchanted, crate and Runeforged items get the Holy Lois tier colour and shine. */
    private static void holyRarity(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        world.getServer().runOnServer(s->{
            var registries=s.registryAccess();
            java.util.function.Function<String,ItemStack> item=id->new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
            java.util.function.Function<ItemStack,Integer> nameColor=stack->{var c=stack.getStyledHoverName().getStyle().getColor();return c==null?-1:c.getValue();};
            int green=holylois.boombox.FishData.color("uncommon")&0xFFFFFF,
                purple=holylois.boombox.FishData.color("epic")&0xFFFFFF,gold=holylois.boombox.FishData.color("legendary")&0xFFFFFF;
            var plain=item.apply("minecraft:diamond_sword");
            check(holylois.boombox.HolyRarity.of(plain).isEmpty()&&(nameColor.apply(plain)==-1||nameColor.apply(plain)==0xFFFFFF),"a plain diamond sword keeps a plain name ("+holylois.boombox.HolyRarity.of(plain)+" "+Integer.toHexString(nameColor.apply(plain))+")");
            var enchanted=item.apply("minecraft:diamond_sword");
            enchanted.enchant(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS),3);
            check(nameColor.apply(enchanted)==green,"an enchanted sword is Uncommon green, not vanilla aqua ("+holylois.boombox.HolyRarity.of(enchanted)+" "+enchanted.getStyledHoverName()+")");
            check(nameColor.apply(item.apply("minecraft:enchanted_golden_apple"))==gold&&holylois.boombox.HolyRarity.of(item.apply("minecraft:mace")).equals("legendary"),"enchanted golden apple and mace are Legendary (item floors)");
            var ours=java.util.Set.of("uncommon","rare","epic","legendary","mythic","cursed").stream().map(t->holylois.boombox.FishData.color(t)&0xFFFFFF).collect(java.util.stream.Collectors.toSet());
            for(int affixes=1;affixes<=8;affixes++){int c=com.ultimatearpg.monster.ArpgEliteMonsterService.eliteColor(affixes);
                check(ours.contains(c)||c==0xFFFFFF,"elite name tags use our tier colours ("+affixes+" affixes: "+Integer.toHexString(c)+")");}
            check(holylois.boombox.HolyRarity.of(item.apply("runeforged:amethyst_sword")).equals("uncommon")&&holylois.boombox.HolyRarity.of(item.apply("runeforged:shaping_stone")).equals("uncommon")
                &&holylois.boombox.HolyRarity.of(item.apply("runeforged:elite_zombie_spawn_egg_cursed")).equals("mythic"),"gem weapons and basic stones are Uncommon, the Cursed Elite egg Mythic");
            for(String id:new String[]{"minecraft:dirt","minecraft:diamond_sword","runeforged:amethyst_sword"}){
                var f=com.anthonyhilyard.legendarytooltips.LegendaryTooltips.getDefinitionColors(item.apply(id),0xF0100010,0xF0100010,0x505000FF,0x5028007F,registries);
                check(f==null||f.index()==-1,"below Legendary keeps the plain standard border ("+id+" "+(f==null?"none":f.index())+")");
            }
            var epic=item.apply("minecraft:stick");epic.set(net.minecraft.core.component.DataComponents.RARITY,net.minecraft.world.item.Rarity.EPIC);
            check(nameColor.apply(epic)==purple,"a vanilla Epic item is our Epic purple");
            var link=epic.getDisplayName().getStyle().getColor();
            check(link!=null&&link.getValue()==purple,"chat item links use the tier colour too");
            check(nameColor.apply(item.apply("fishing_crates:wooden_crate"))==green,"a wooden crate is Uncommon");
            var rune=item.apply("minecraft:iron_sword");
            com.ultimatearpg.item.ArpgWeaponHelper.setQuality(rune,com.ultimatearpg.item.ArpgItemQuality.LEGENDARY);
            var runeName=rune.getStyledHoverName();
            check(holylois.boombox.HolyRarity.of(rune).equals("legendary")&&runeName.getStyle().getColor()!=null&&runeName.getStyle().getColor().getValue()==gold,
                "a Runeforged legendary is our Legendary gold ("+runeName.getStyle()+")");
            check(holylois.boombox.FishData.SHINE_LEGENDARY.equals(((net.minecraft.network.chat.FontDescription.Resource)runeName.getStyle().getFont()).id()),"Legendary names shine");
            var magic=item.apply("minecraft:iron_sword");
            com.ultimatearpg.item.ArpgWeaponHelper.setQuality(magic,com.ultimatearpg.item.ArpgItemQuality.MAGIC);
            check(nameColor.apply(magic)==green,"a Runeforged magic item is Uncommon green");
            var lootboxTool=item.apply("minecraft:diamond_pickaxe");
            lootboxTool.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Tester's Lucky Pickaxe").withStyle(st->st.withColor(0xFFAA00)));
            check(holylois.boombox.HolyRarity.of(lootboxTool).equals("legendary"),"a gold or orange named item (lootbox tools) is Legendary");
            var runeRare=item.apply("minecraft:iron_sword");
            com.ultimatearpg.item.ArpgWeaponHelper.setQuality(runeRare,com.ultimatearpg.item.ArpgItemQuality.RARE);
            var lines=runeRare.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(registries),null,net.minecraft.world.item.TooltipFlag.NORMAL);
            var title=lines.getFirst().getStyle().getColor();
            check(title!=null&&title.getValue()==(holylois.boombox.FishData.color("rare")&0xFFFFFF),"Runeforged tooltip titles use our tier colour ("+lines.getFirst()+")");
            var qualityLine=lines.stream().filter(l->l.getString().startsWith("Quality:")).findFirst();
            check(qualityLine.isEmpty()||qualityLine.get().getString().equals("Quality: Rare")&&qualityLine.get().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents q
                &&q.getArgs()[0] instanceof net.minecraft.network.chat.Component word&&word.getStyle().getColor()!=null&&word.getStyle().getColor().getValue()==(holylois.boombox.FishData.color("rare")&0xFFFFFF),
                "the Runeforged quality line names and colours our tier ("+qualityLine.map(Object::toString).orElse("none")+")");
            var cursed=item.apply("minecraft:iron_sword");
            com.ultimatearpg.item.ArpgWeaponHelper.setQuality(cursed,com.ultimatearpg.item.ArpgItemQuality.CURSED);
            check(holylois.boombox.HolyRarity.of(cursed).equals("cursed")&&nameColor.apply(cursed)==(holylois.boombox.FishData.color("cursed")&0xFFFFFF),"Runeforged cursed gear is our Cursed violet");
            java.util.function.Function<String,ItemStack> trophy=tier->{var cod=item.apply("minecraft:cod");var tag=new net.minecraft.nbt.CompoundTag();var fishTag=new net.minecraft.nbt.CompoundTag();
                fishTag.putString("rarity",tier);fishTag.putDouble("kg",2.0);tag.put("holylois_fish",fishTag);
                cod.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
                cod.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);return cod;};
            check(!trophy.apply("rare").hasFoil()&&trophy.apply("mythic").hasFoil(),"trophy fish glint only when Mythic (or Shiny)");
            check(holylois.boombox.RarityOutline.of(enchanted)==0&&holylois.boombox.RarityOutline.of(trophy.apply("uncommon"))!=0&&holylois.boombox.RarityOutline.of(runeRare)!=0,
                "Uncommon gear has no inventory outline; fish and Rare gear do");
            var book=item.apply("minecraft:enchanted_book");
            book.set(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS,enchanted.getEnchantments());
            var bookFrame=com.anthonyhilyard.legendarytooltips.LegendaryTooltips.getDefinitionColors(book,0,0,0,0,registries);
            check(holylois.boombox.RarityOutline.of(book)==0&&(bookFrame==null||bookFrame.index()<0),
                "enchanted books: no outline and no spike frame (tier "+holylois.boombox.HolyRarity.of(book)+", frame "+(bookFrame==null?"none":bookFrame.index())+")");
            var legendaryFish=trophy.apply("legendary");var mythicFish=trophy.apply("mythic");
            var legendaryFrame=com.anthonyhilyard.legendarytooltips.LegendaryTooltips.getDefinitionColors(legendaryFish,0,0,0,0,registries);
            var mythicFrame=com.anthonyhilyard.legendarytooltips.LegendaryTooltips.getDefinitionColors(mythicFish,0,0,0,0,registries);
            check(legendaryFrame!=null&&legendaryFrame.index()>=0&&mythicFrame!=null&&mythicFrame.index()==0&&mythicFrame.resource().getNamespace().equals("holylois"),
                "Legendary fish get gold spikes and Mythic fish our red frame");
        });
    }

    /** A trophy pufferfish in either hand (here the off hand) hurts a melee attacker back, and thorns never answer thorns. */
    private static void pufferThorns(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        var server=world.getServer();
        server.runCommand("gamemode survival @a");
        float[] health=new float[2];java.util.UUID[] id=new java.util.UUID[1];
        server.runOnServer(s->{
            var player=s.getPlayerList().getPlayers().getFirst();var level=(ServerLevel)player.level();
            var puffer=new ItemStack(net.minecraft.world.item.Items.PUFFERFISH);var tag=new net.minecraft.nbt.CompoundTag();var fishTag=new net.minecraft.nbt.CompoundTag();
            fishTag.putString("rarity","legendary");fishTag.putDouble("kg",3.0);tag.put("holylois_fish",fishTag);
            fishTag.putString("species","minecraft:pufferfish");fishTag.putBoolean("held_tooltip",true);tag.put("holylois_fish",fishTag);
            puffer.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
            puffer.set(net.minecraft.core.component.DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(List.of(net.minecraft.network.chat.Component.literal("Caught by TestAngler, 2026-10-19"))));
            holylois.boombox.LowTierFish.normalize(puffer);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,puffer);
            var zombie=(net.minecraft.world.entity.LivingEntity)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:zombie")).create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            zombie.setPos(player.getX()+1,player.getY(),player.getZ());level.addFreshEntity(zombie);id[0]=zombie.getUUID();
            health[0]=zombie.getHealth();
            player.hurtServer(level,level.damageSources().thorns(zombie),1);
            check(zombie.getHealth()==health[0],"thorns damage is not reflected again");
        });
        context.waitTicks(15);
        server.runOnServer(s->{
            var player=s.getPlayerList().getPlayers().getFirst();var level=(ServerLevel)player.level();
            var zombie=(net.minecraft.world.entity.LivingEntity)level.getEntity(id[0]);
            check(player.getArmorValue()>0,"a trophy pufferfish in the off hand gives its armor ("+player.getArmorValue()+")");
            player.hurtServer(level,level.damageSources().mobAttack(zombie),1);
            check(zombie.getHealth()<health[0],"a Legendary pufferfish in the off hand hurts the zombie that hit you ("+health[0]+" -> "+zombie.getHealth()+")");
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY);player.setHealth(player.getMaxHealth());zombie.discard();
        });
    }

    /** Screenshots of real tooltips (frame, spikes, colours) for owner review: one per tier example, hovered in the inventory. */
    private static void tierTooltipShots(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        var server = world.getServer();
        server.runCommand("gamemode survival @a");
        String[] kinds = {"dirt", "amethyst-sword", "beacon", "golden-apple", "cursed-egg", "rod", "book", "lootbox-pickaxe", "rune-magic", "rune-legendary", "rune-cursed", "fish-legendary", "fish-mythic"};
        for (String kind : kinds) {
            server.runCommand("clear @a");
            server.runOnServer(s -> {
                var player = s.getPlayerList().getPlayers().getFirst(); var registries = s.registryAccess();
                var sharp = registries.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                ItemStack stack = switch (kind) {
                    case "rod" -> { var r = new ItemStack(net.minecraft.world.item.Items.FISHING_ROD); r.enchant(sharp.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LURE), 3); yield r; }
                    case "book" -> { var b = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK); var e = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
                        e.set(sharp.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.MENDING), 1); b.set(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, e.toImmutable()); yield b; }
                    case "lootbox-pickaxe" -> { var t = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE); t.enchant(sharp.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 2);
                        t.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Tester's Lucky Pickaxe").withStyle(st -> st.withColor(0xFFAA00).withItalic(false))); yield t; }
                    case "dirt" -> new ItemStack(net.minecraft.world.item.Items.DIRT);
                    case "beacon" -> new ItemStack(net.minecraft.world.item.Items.BEACON);
                    case "golden-apple" -> new ItemStack(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE);
                    case "amethyst-sword" -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("runeforged:amethyst_sword")));
                    case "cursed-egg" -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("runeforged:elite_zombie_spawn_egg_cursed")));
                    case "fish-legendary", "fish-mythic" -> { var cod = new ItemStack(net.minecraft.world.item.Items.COD); var tag = new net.minecraft.nbt.CompoundTag(); var fishTag = new net.minecraft.nbt.CompoundTag();
                        fishTag.putString("rarity", kind.substring(5)); fishTag.putDouble("kg", kind.endsWith("mythic") ? 60 : 25); tag.put("holylois_fish", fishTag);
                        cod.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag)); yield cod; }
                    default -> { var sword = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
                        com.ultimatearpg.monster.ArpgGeneratedWeaponService.applyQualityAndRollAffixes(sword, switch (kind) { case "rune-magic" -> com.ultimatearpg.item.ArpgItemQuality.MAGIC;
                            case "rune-legendary" -> com.ultimatearpg.item.ArpgItemQuality.LEGENDARY; default -> com.ultimatearpg.item.ArpgItemQuality.CURSED; }, player.getRandom()); yield sword; }
                };
                player.getInventory().setItem(20, stack);
            });
            context.waitTicks(5);
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitTicks(5);
            var pos = context.computeOnClient(client -> {
                var screen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) client.gui.screen();
                Slot target = null;
                for (var slot : screen.getMenu().slots)
                    if (slot.container instanceof net.minecraft.world.entity.player.Inventory && slot.getContainerSlot() == 20) target = slot;
                int left = (int) field(screen, "leftPos"), top = (int) field(screen, "topPos");
                double scale = client.getWindow().getGuiScale();
                return new double[] {(left + target.x + 8) * scale, (top + target.y + 8) * scale};
            });
            context.getInput().setCursorPos(pos[0], pos[1]);
            context.waitTicks(8);
            context.takeScreenshot("tier-" + kind);
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitTicks(3);
        }
        server.runCommand("clear @a");
    }

    /** Catch the fish you see: the closest wild fish within reach of the bobber; bucket fish and far fish never count. */
    private static void seenCatch(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        world.getServer().runCommand("clear @a");
        world.getServer().runOnServer(s->{
            var player=s.getPlayerList().getPlayers().getFirst();
            var level=(ServerLevel)player.level();
            var origin=player.blockPosition().offset(12,0,0);
            var before=new java.util.LinkedHashMap<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState>();
            for(var pos:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-5,-1,-2),origin.offset(5,1,2))){before.put(pos.immutable(),level.getBlockState(pos));level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());}
            var bobber=net.minecraft.world.phys.Vec3.atCenterOf(origin);
            java.util.function.BiFunction<Double,Boolean,net.minecraft.world.entity.animal.fish.AbstractFish> cod=(dx,bucket)->{
                var fish=(net.minecraft.world.entity.animal.fish.AbstractFish)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:cod")).create(level,net.minecraft.world.entity.EntitySpawnReason.NATURAL);
                fish.setNoAi(true);fish.setPos(bobber.add(dx,0,0));fish.setFromBucket(bucket);level.addFreshEntity(fish);return fish;
            };
            var released=cod.apply(0.5,true);var wild=cod.apply(-1.5,false);var far=cod.apply(4.5,false);
            var hook=new net.minecraft.world.entity.projectile.FishingHook(player,level,0,0);hook.setPos(bobber);level.addFreshEntity(hook);
            var seen=holylois.boombox.SchoolCatch.seen(hook);
            check(seen==wild,"the bite takes the wild fish next to the bobber, not the closer released one ("+seen+")");
            released.discard();wild.discard();
            check(holylois.boombox.SchoolCatch.seen(hook)==null,"a fish 4.5 blocks away is out of reach, so the normal loot roll stays");
            far.discard();hook.discard();
            before.forEach(level::setBlockAndUpdate);
        });
        world.getServer().runCommand("clear @a");
    }

    private static void shoalCatch(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        world.getServer().runCommand("clear @a");
        world.getServer().runOnServer(s->{
            var player=s.getPlayerList().getPlayers().getFirst();
            var level=(ServerLevel)player.level();
            var type=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("fishofthieves:shoal"));
            var shoal=new com.stevekung.fishofthieves.entity.shoal.Shoal(type,level);
            shoal.setPos(player.getX(),player.getY(),player.getZ());
            shoal.getShoalFish().add(new com.stevekung.fishofthieves.entity.shoal.ShoalFishData("fishofthieves:splashtail",java.util.UUID.randomUUID(),new net.minecraft.nbt.CompoundTag()));
            level.addFreshEntity(shoal);
            var hook=new net.minecraft.world.entity.projectile.FishingHook(player,level,0,0);
            hook.setPos(shoal.position());level.addFreshEntity(hook);
            int before=player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.FISH_CAUGHT));
            int result=com.stevekung.fishofthieves.entity.shoal.Shoal.fishUpShoal(hook,player);
            check(result==4&&!hook.isAlive(),"real FoT shoal retrieval retains rod cost and removes the hook");
            check(player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.FISH_CAUGHT))==before+1,"shoal catch counted once");
            int caught=0;for(int i=0;i<player.getInventory().getContainerSize();i++){
                var item=player.getInventory().getItem(i);
                if(BuiltInRegistries.ITEM.getKey(item.getItem()).toString().equals("fishofthieves:splashtail"))caught+=item.getCount();
            }
            check(caught==0,"shoal catch does not insert an item instantly into inventory");
            var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,player.getBoundingBox().inflate(8));
            check(drops.stream().filter(d->BuiltInRegistries.ITEM.getKey(d.getItem().getItem()).toString().equals("fishofthieves:splashtail")).count()==1,"shoal catch launches exactly one fish item");
            drops.forEach(net.minecraft.world.entity.Entity::discard);
            check(level.getEntitiesOfClass(net.minecraft.world.entity.animal.fish.AbstractFish.class,player.getBoundingBox().inflate(8)).isEmpty(),"shoal catch does not spawn a live fish");
            shoal.discard();
        });
        world.getServer().runCommand("clear @a");
    }

    private static void fishVariantModels(ClientGameTestContext context,TestSingleplayerContext world){
        context.runOnClient(client->{
            var raw=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("fishofthieves:splashtail")));
            var data=new net.minecraft.nbt.CompoundTag();data.putString("splashtail_variant","fishofthieves:ruby");
            raw.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
            var trophy=raw.copy();var tag=data.copy();var weight=new net.minecraft.nbt.CompoundTag();
            weight.putString("rarity","rare");weight.putDouble("kg",2);tag.put("holylois_fish",weight);
            trophy.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
            var pictures=new ArrayList<String>();
            for(var stack:List.of(raw,trophy)){
                var state=new net.minecraft.client.renderer.item.ItemStackRenderState();
                client.getItemModelResolver().updateForLiving(state,stack,net.minecraft.world.item.ItemDisplayContext.GUI,client.player);
                pictures.add(state.pickParticleMaterial(client.player.getRandom()).sprite().contents().name().toString());
            }
            log("FoT variant textures: plain="+pictures.get(0)+" weighed="+pictures.get(1));
            check(pictures.get(0).contains("ruby_splashtail")&&pictures.get(0).equals(pictures.get(1)),"rarity data does not replace the visible FoT variant texture");
        });
    }

    private static void uiRefinements(ClientGameTestContext context,TestSingleplayerContext world){
        context.runOnClient(client->{
            check(!me.shedaniel.rei.api.client.config.ConfigObject.getInstance().isFavoritesEnabled(),"REI favorites and its left panel are disabled");
            client.gui.setScreen(new InventoryScreen(client.player));
        });
        context.waitTicks(5);context.takeScreenshot("55-no-favorites");
        context.runOnClient(client->{
            var entry=new holylois.boombox.JournalState.Entry("first",2,"Ruin Town",client.player.getName().getString(),"minecraft:overworld",8,63,2,1,1,0,0,0,true,true);
            var screen=new holylois.boombox.JournalScreen(new holylois.boombox.JournalState(1,2,0,1,1,8,"",List.of(entry)));
            client.gui.setScreen(screen);
            try{
                var title=holylois.boombox.JournalScreen.class.getDeclaredMethod("title",holylois.boombox.JournalState.Entry.class);title.setAccessible(true);
                var text=(net.minecraft.network.chat.Component)title.invoke(null,entry);
                check(!text.getString().contains("★")&&text.getStyle().getColor().getValue()==(holylois.boombox.Ui.GOLD&0xFFFFFF),"first discovery name is clean yellow without a star");
                var summary=holylois.boombox.JournalScreen.class.getDeclaredMethod("summary",holylois.boombox.JournalState.Entry.class);summary.setAccessible(true);
                check(((net.minecraft.network.chat.Component)summary.invoke(screen,entry)).getString().startsWith("★ "),"world-first star appears in the description");
                var kill=new holylois.boombox.JournalState.Entry("kill",3,"Zombie",entry.by(),"minecraft:overworld",8,63,2,1,5,0,0,16,true,true);
                var count=(net.minecraft.network.chat.Component)summary.invoke(screen,kill);
                check(!count.getString().contains("★")&&count.getStyle().getColor()==null,"personal defeat count is plain, without gold or a star");
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        });
        context.waitTicks(5);context.takeScreenshot("55-first-description");
        context.runOnClient(client->client.gui.setScreen(null));
    }

    private static void lowTierPolish(ClientGameTestContext context,TestSingleplayerContext world){
        world.getServer().runCommand("clear @a");
        world.getServer().runOnServer(server->{
            var player=server.getPlayerList().getPlayers().getFirst();
            for(int i=0;i<2;i++){
                var stack=new ItemStack(net.minecraft.world.item.Items.TROPICAL_FISH,2);var root=new net.minecraft.nbt.CompoundTag();var fish=new net.minecraft.nbt.CompoundTag();
                fish.putString("rarity",i==0?"common":"uncommon");fish.putDouble("kg",0.15+i);root.put("holylois_fish",fish);
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(root));
                stack.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE,1);
                stack.set(net.minecraft.core.component.DataComponents.TROPICAL_FISH_PATTERN,net.minecraft.world.entity.animal.fish.TropicalFish.Pattern.FLOPPER);
                var other=stack.copy();var different=root.copy();var old=different.getCompound("holylois_fish").orElseThrow();old.putDouble("kg",0.9);old.putString("by","AnotherAngler");different.put("holylois_fish",old);
                other.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(different));
                holylois.boombox.LowTierFish.normalize(stack);holylois.boombox.LowTierFish.normalize(other);
                check(ItemStack.isSameItemSameComponents(stack,other),"old low-tier catches stack despite different weight/owner");
                check(!holylois.boombox.LowTierFish.normalize(stack),"migration is idempotent");
                player.getInventory().setItem(20+i,stack);
            }
            player.inventoryMenu.broadcastChanges();
        });
        context.waitTicks(5);
        context.runOnClient(client->{
            for(int i=20;i<22;i++){
                var stack=client.player.getInventory().getItem(i);
                check(stack.getMaxStackSize()==64&&!holylois.boombox.FishData.weighed(stack),"low-tier food stacks without weight");
                String lines=stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(client.level),client.player,net.minecraft.world.item.TooltipFlag.NORMAL).toString();
                check(!lines.contains("Parrotfish")&&!lines.contains("Weight:"),"no bucket flavor or weight on food");
                if(i==21)check(holylois.boombox.RarityOutline.of(stack)==(0xFE000000|(holylois.boombox.FishData.color("uncommon")&0xFFFFFF)),"Uncommon has green sprite outline");
            }
            for(String name:List.of("Golden Crate","Raw Cod","Legendary long fish name"))for(String rarity:List.of("legendary","mythic"))for(int origin:new int[]{20,295,610}){
                var text=net.minecraft.network.chat.Component.literal(name).withStyle(s->holylois.boombox.FishData.shine(s.withBold(rarity.equals("mythic")),rarity));
                var prepared=client.font.prepareText(text.getVisualOrderText(),origin,20,0xFFFFFFFF,false,false,0);
                @SuppressWarnings("unchecked") var runs=(List<holylois.boombox.ShineText.Run>)field(prepared,"holyLoisRuns");
                check(runs.size()==1&&runs.getFirst().start()==0&&runs.getFirst().end()==client.font.getSplitter().stringWidth(text.getVisualOrderText()),"shine covers full name independently of screen position, including last letter");
            }
            client.gui.setScreen(new InventoryScreen(client.player));
        });
        context.waitTicks(5);context.takeScreenshot("57-stackable-low-fish");
        var pos=context.computeOnClient(client->{
            var screen=(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)client.gui.screen();
            var slot=screen.getMenu().slots.stream().filter(s->s.container instanceof net.minecraft.world.entity.player.Inventory&&s.getContainerSlot()==21).findFirst().orElseThrow();
            return new double[]{((int)field(screen,"leftPos")+slot.x+8)*client.getWindow().getGuiScale(),((int)field(screen,"topPos")+slot.y+8)*client.getWindow().getGuiScale()};
        });
        context.getInput().setCursorPos(pos[0],pos[1]);context.waitTicks(5);context.takeScreenshot("57-uncommon-tooltip");
        context.runOnClient(client->client.gui.setScreen(null));
    }

    private static void fishPickupSections(ClientGameTestContext context,TestSingleplayerContext world) throws Exception {
        var server=world.getServer();server.runCommand("clear @a");
        server.runOnServer(s->{
            var player=s.getPlayerList().getPlayers().getFirst();var level=(ServerLevel)player.level();
            var fish=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("fishofthieves:splashtail")));
            var data=new net.minecraft.nbt.CompoundTag();data.putString("splashtail_variant","fishofthieves:ruby");
            var meal=new net.minecraft.nbt.CompoundTag();meal.putString("rarity","uncommon");meal.putDouble("kg",0.8);meal.putString("by","EarlierAngler");data.put("holylois_fish",meal);
            fish.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
            fish.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE,1);player.getInventory().setItem(20,fish);
            var fresh=fish.copy();var tag=data.copy();tag.getCompound("holylois_fish").orElseThrow().putString("by","AnotherAngler");
            fresh.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
            var drop=new net.minecraft.world.entity.item.ItemEntity(level,player.getX(),player.getY(),player.getZ(),fresh);
            level.addFreshEntity(drop);drop.setNoPickUpDelay();drop.playerTouch(player);
            log("FoT Uncommon pickup count="+player.getInventory().getItem(20).getCount()+" max="+player.getInventory().getItem(20).getMaxStackSize());
            check(player.getInventory().getItem(20).getCount()==2&&!drop.isAlive(),"actual FoT Uncommon pickup merges into old stack before periodic sweep");
            var raw=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("fishofthieves:islehopper")));
            var trophy=new net.minecraft.nbt.CompoundTag();var f=new net.minecraft.nbt.CompoundTag();f.putString("rarity","legendary");f.putString("species","fishofthieves:islehopper");f.putDouble("kg",36.47);trophy.put("holylois_fish",f);
            raw.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(trophy));
            try{
                var apply=holylois.boombox.FishTraits.class.getDeclaredMethod("apply",ItemStack.class,String.class,String.class,double.class,boolean.class,boolean.class);apply.setAccessible(true);
                @SuppressWarnings("unchecked") var effects=(List<net.minecraft.network.chat.Component>)apply.invoke(null,raw,"fishofthieves:islehopper","legendary",0.98,false,false);
                var lore=new ArrayList<>(effects);lore.add(net.minecraft.network.chat.Component.literal("Caught by TestAngler, 2026-10-12"));
                raw.set(net.minecraft.core.component.DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(lore));
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            holylois.boombox.LowTierFish.normalize(raw);player.getInventory().setItem(21,raw);
            var cooked=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("fishofthieves:cooked_islehopper")));
            holylois.boombox.Legends.cooked(raw,cooked);
            check(cooked.get(net.minecraft.core.component.DataComponents.LORE).lines().stream().filter(l->l.getString().equals("When held:")).count()==1,"cooking rebuilds one held section after copying trophy data");
            var slice=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("farmersdelight:cod_slice")));
            holylois.boombox.Legends.fillet(raw,slice);
            check(!slice.has(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS)||slice.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).modifiers().isEmpty(),"fillets have no whole-fish held attribute");
            check(slice.get(net.minecraft.core.component.DataComponents.LORE).lines().stream().anyMatch(l->l.getString().contains("Jump Boost")),"fillets retain the shorter eaten Jump Boost");
            player.getInventory().setItem(22,slice);
            var map=new ItemStack(net.minecraft.world.item.Items.BURIED_TREASURE_MAP);map.set(net.minecraft.core.component.DataComponents.MAP_ID,new net.minecraft.world.level.saveddata.maps.MapId(2));
            player.getInventory().setItem(23,map);player.inventoryMenu.broadcastChanges();
        });
        context.waitTicks(5);
        context.runOnClient(client->{
            var raw=client.player.getInventory().getItem(21);
            var lines=raw.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(client.level),client.player,net.minecraft.world.item.TooltipFlag.NORMAL).stream().map(net.minecraft.network.chat.Component::getString).toList();
            log("islehopper sections: "+lines);
            int held=lines.indexOf("When held:");int caught=-1;for(int i=0;i<lines.size();i++)if(lines.get(i).startsWith("Caught by "))caught=i;
            check(held>=0&&caught>held&&lines.subList(caught+1,lines.size()).stream().allMatch(String::isBlank),"held attribute is labeled and catch credit is last text before food icons");
            var map=client.player.getInventory().getItem(23);var tooltip=map.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(client.level),client.player,net.minecraft.world.item.TooltipFlag.NORMAL).stream().map(net.minecraft.network.chat.Component::getString).toList();
            log("treasure map tooltip: "+tooltip);
            check(map.getStyledHoverName().getString().equals("Buried Treasure Map")&&tooltip.stream().noneMatch(t->t.startsWith("ID #")||t.contains("filled_map."))&&tooltip.contains("The number 2 is etched into the map."),"treasure map has a readable name and real numbered inscription");
            client.gui.setScreen(new InventoryScreen(client.player));
        });
        context.waitTicks(5);
        for(int item:new int[]{21,22,23}){
            var pos=context.computeOnClient(client->{var screen=(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)client.gui.screen();
                var slot=screen.getMenu().slots.stream().filter(s->s.container instanceof net.minecraft.world.entity.player.Inventory&&s.getContainerSlot()==item).findFirst().orElseThrow();
                return new double[]{((int)field(screen,"leftPos")+slot.x+8)*client.getWindow().getGuiScale(),((int)field(screen,"topPos")+slot.y+8)*client.getWindow().getGuiScale()};});
            context.getInput().setCursorPos(pos[0],pos[1]);context.waitTicks(4);context.takeScreenshot("58-tooltip-section-"+item);
        }
        context.runOnClient(client->client.gui.setScreen(null));
    }

    private static void shoalIdentity(ClientGameTestContext context,TestSingleplayerContext world){
        var server=world.getServer();
        server.runCommand("clear @a");
        // The preceding shoal catch moves the angler far away. This pond must be inside the client's loaded area.
        server.runCommand("tp @a 6 -59 -3 -90 12");
        server.runCommand("fill 3 -64 -5 18 -59 8 water");
        // Waiting on the old rendered chunks can return before the teleport packet has reached the client.
        context.waitFor(client->client.player.blockPosition().closerThan(new BlockPos(6,-59,-3),2),200);
        world.getConnection().waitForChunksRender();
        var fixture=new java.util.concurrent.atomic.AtomicInteger();
        server.runOnServer(s->{
            var player=s.getPlayerList().getPlayers().getFirst();var level=(ServerLevel)player.level();
            var type=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("fishofthieves:shoal"));
            var shoal=new com.stevekung.fishofthieves.entity.shoal.Shoal(type,level);shoal.setPos(10,-59,2);
            level.setBlock(new BlockPos(10,-59,2),com.stevekung.fishofthieves.registry.FOTBlocks.SHOAL.defaultBlockState(),3);
            shoal.createNaturalSpawn(false);
            check(shoal.getShoalFish().stream().anyMatch(f->f.id().startsWith("minecraft:")),"natural circling shoals include a biome-appropriate vanilla fish");
            check(shoal.getShoalFish().stream().allMatch(f->f.data().contains(holylois.boombox.SwimmingFish.SAVE_KEY)),"every displayed virtual fish has a saved catch identity before sync");
            var entry=shoal.getShoalFish().getFirst();var tag=entry.data().copy();tag.putString("id",entry.id());
            var fish=(net.minecraft.world.entity.animal.fish.AbstractFish)net.minecraft.world.entity.EntityType.loadEntityRecursive(tag,level,new net.minecraft.world.entity.EntitySpawnRequest(net.minecraft.world.entity.EntitySpawnReason.LOAD,false),net.minecraft.world.entity.EntityProcessor.NOP);
            var item=holylois.boombox.SwimmingFish.identity(fish).copy();
            var data=item.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();var trophy=data.getCompound("holylois_fish").orElseThrow();
            trophy.putString("rarity","mythic");trophy.putDouble("kg",80);trophy.putDouble("swim_kg",80);data.put("holylois_fish",trophy);
            item.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
            ((holylois.boombox.SwimmingFish.Memory)fish).holyLoisFish(item);holylois.boombox.SwimmingFish.applyScale(fish,item);
            var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());fish.saveWithoutId(output);
            var saved=output.buildResult();saved.putString("id",entry.id());
            var loaded=(net.minecraft.world.entity.animal.fish.AbstractFish)net.minecraft.world.entity.EntityType.loadEntityRecursive(saved,level,new net.minecraft.world.entity.EntitySpawnRequest(net.minecraft.world.entity.EntitySpawnReason.LOAD,false),net.minecraft.world.entity.EntityProcessor.NOP);
            check(holylois.boombox.FishData.kilograms(holylois.boombox.SwimmingFish.identity(loaded))==80&&loaded.getScale()>3,"fish weight and body scale survive entity save/load");
            var bucket=loaded.getBucketItemStack();loaded.saveToBucketTag(bucket);
            var released=(net.minecraft.world.entity.animal.fish.AbstractFish)loaded.getType().create(level,net.minecraft.world.entity.EntitySpawnReason.BUCKET);
            released.loadFromBucketTag(bucket.get(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA).copyTag());
            check(holylois.boombox.FishData.kilograms(holylois.boombox.SwimmingFish.identity(released))==80&&released.getScale()>3,"bucket capture/release keeps the fish identity instead of rerolling it");
            shoal.getShoalFish().set(0,new com.stevekung.fishofthieves.entity.shoal.ShoalFishData(entry.id(),entry.uuid(),saved));
            check(holylois.boombox.ShoalFishIdentity.radius(shoal)>2,"a giant fish widens its school's circle/bubbles");
            check(level.addFreshEntity(shoal),"the test shoal is added to the loaded pond");fixture.set(shoal.getId());
        });
        context.runOnClient(client->{client.player.setYRot(-90);client.player.setXRot(12);});
        context.waitFor(client->client.level.getEntity(fixture.get()) instanceof com.stevekung.fishofthieves.entity.shoal.Shoal,200);
        context.waitFor(client->client.level.getEntity(fixture.get()) instanceof com.stevekung.fishofthieves.entity.shoal.Shoal shoal
            &&shoal.getShoalFishClient().stream().anyMatch(f->f.getScale()>3),200);
        context.runOnClient(client->{
            var shoals=client.level.getEntitiesOfClass(com.stevekung.fishofthieves.entity.shoal.Shoal.class,new net.minecraft.world.phys.AABB(2,-65,-6,20,-55,10));
            log("client shoal diagnostics: entities="+shoals.size()+" request channel="+net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(com.stevekung.fishofthieves.network.RequestServerShoalFishPacket.TYPE));
            for(var shoal:shoals){
                log("client shoal id="+shoal.getId()+" fish="+shoal.getShoalFishClient().size());
                for(var fish:shoal.getShoalFishClient())log("client virtual fish: "+BuiltInRegistries.ENTITY_TYPE.getKey(fish.getType())+" scale="+fish.getScale()+" kg="+holylois.boombox.FishData.kilograms(((holylois.boombox.SwimmingFish.Memory)fish).holyLoisFish()));
            }
        });
        context.takeScreenshot("56-mixed-large-shoal");
        context.runOnClient(client->{
            var shoals=client.level.getEntitiesOfClass(com.stevekung.fishofthieves.entity.shoal.Shoal.class,new net.minecraft.world.phys.AABB(2,-65,-6,20,-55,10));
            check(!shoals.isEmpty()&&shoals.getFirst().getShoalFishClient().stream().anyMatch(f->f.getScale()>3),"client virtual fish uses the saved large scale");
            check(shoals.getFirst().getShoalFishClient().stream().anyMatch(f->BuiltInRegistries.ENTITY_TYPE.getKey(f.getType()).getNamespace().equals("minecraft")),"vanilla fish renders inside the shoal");
        });
        server.runOnServer(s->{
            var player=s.getPlayerList().getPlayers().getFirst();var level=(ServerLevel)player.level();
            var shoal=level.getEntitiesOfClass(com.stevekung.fishofthieves.entity.shoal.Shoal.class,new net.minecraft.world.phys.AABB(2,-65,-6,20,-55,10)).getFirst();
            var entry=shoal.getShoalFish().getFirst();shoal.getShoalFish().clear();shoal.getShoalFish().add(entry);
            var hook=new net.minecraft.world.entity.projectile.FishingHook(player,level,0,0);hook.setPos(shoal.position().add(holylois.boombox.ShoalFishIdentity.radius(shoal)-0.25,0,0));level.addFreshEntity(hook);
            check(com.stevekung.fishofthieves.entity.shoal.Shoal.fishUpShoal(hook,player)==4,"a cast near the enlarged visible circle's edge reels its fish");
            var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(2,-65,-6,20,-55,10));
            var caught=drops.stream().filter(d->holylois.boombox.FishData.kilograms(d.getItem())==80).findFirst().orElseThrow();
            check(holylois.boombox.FishData.rarity(caught.getItem()).equals("mythic")&&caught.getDeltaMovement().lengthSqr()>0,"caught item keeps the visible rarity/weight and flies toward the angler");
            caught.discard();
        });
        server.runCommand("fill 3 -64 -5 18 -59 8 air");
    }

    private static void fishingHistory(ClientGameTestContext context,TestSingleplayerContext world){
        context.runOnClient(client->{
            var entry=new holylois.boombox.JournalState.Entry("fish",1,"Cod","","",0,0,0,0,1,10,8,0,false,false,List.of(0L,0L,0L,1L,0L,0L),0);
            var lines=holylois.boombox.JournalScreen.rarityLines(entry);
            check(lines.size()==4,"an Epic first catch reveals exactly Common through Epic");
            for(int i=0;i<3;i++)check(lines.get(i).getString().endsWith(": 0"),"missed lower tier "+i+" shows zero");
            check(lines.get(3).getString().endsWith(": 1"),"the caught Epic shows its real count");
            var old=new holylois.boombox.JournalState.Entry("fish",1,"Cod","","",0,0,0,0,1,10,8,0,false,false);
            check(!holylois.boombox.JournalScreen.rarityLines(old).get(3).getString().endsWith(": 0"),"old untracked Epic count remains unknown");
        });
    }

    private static void cratePresentation(ClientGameTestContext context,TestSingleplayerContext world) throws Exception {
        for(String type:new String[]{"wooden","iron","golden"}){
            context.runOnClient(client->{
                var item=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("fishing_crates:"+type+"_crate")));
                String tier=holylois.boombox.CrateStyle.rarity(item);
                var lines=item.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(client.level),client.player,net.minecraft.world.item.TooltipFlag.NORMAL);
                check(lines.getFirst().getStyle().getColor().getValue()==(holylois.boombox.FishData.color(tier)&0xFFFFFF),type+" crate tooltip uses "+tier+" color");
                check(lines.get(1).getString().equals(net.minecraft.network.chat.Component.translatable("holylois.crate.tip").getString())&&lines.get(2).getString().equals(net.minecraft.network.chat.Component.translatable("holylois.crate.open").getString()),"crate tooltip explains placing and breaking for loot");
                log(type+" held crate name: "+item.getStyledHoverName()+" expected="+Integer.toHexString(holylois.boombox.FishData.color(tier)&0xFFFFFF));
                check(item.getStyledHoverName().getStyle().getColor().getValue()==(holylois.boombox.FishData.color(tier)&0xFFFFFF),type+" crate held name uses its tier color");
                var border=com.anthonyhilyard.legendarytooltips.LegendaryTooltips.getDefinitionColors(item,0xF0100010,0xF0100010,0x505000FF,0x5028007F,client.level.registryAccess());
                check(border.startBorder().get()==holylois.boombox.FishData.color(tier)&&border.endBorder().get()==holylois.boombox.FishData.color(tier),type+" crate tooltip border uses the shared fish palette");
                try {
                    var cards=Class.forName("holylois.boombox.ItemCards");
                    var clear=cards.getDeclaredMethod("clear");clear.setAccessible(true);clear.invoke(null);
                    var last=cards.getDeclaredField("lastShown");last.setAccessible(true);last.setLong(null,0);
                    var pick=cards.getDeclaredMethod("pickedUp",ItemStack.class);pick.setAccessible(true);pick.invoke(null,item);
                } catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            });
            context.waitTicks(12);context.takeScreenshot("54-crate-"+type);
            world.getServer().runCommand("item replace entity @p container.20 with fishing_crates:"+type+"_crate");
            context.waitTicks(4);
            context.runOnClient(client->client.gui.setScreen(new InventoryScreen(client.player)));
            var point=context.computeOnClient(client->{
                var screen=(InventoryScreen)client.gui.screen();var slot=screen.getMenu().getSlot(20);
                double scale=client.getWindow().getGuiScale();
                return new double[]{((int)field(screen,"leftPos")+slot.x+8)*scale,((int)field(screen,"topPos")+slot.y+8)*scale};
            });
            context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(5);
            context.takeScreenshot("54-crate-tooltip-"+type);
            context.runOnClient(client->client.gui.setScreen(null));
        }
    }

    /** The owner's repro: standing on a 4-block pillar out of the water, F5, tilting so the camera passes the pillar top. */
    private static void pillarCamera(ClientGameTestContext context, net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server, String tag) {
        // A real ocean's height (surface y 63) and the pack's render distance 12, as in the owner's world.
        context.runOnClient(client -> client.options.renderDistance().set(12));
        for (String c : new String[] {"gamemode survival @a", "fill 0 55 0 63 55 63 sand", "fill 0 56 0 63 62 63 water",
                                      "fill 32 56 20 32 66 20 stone", "tp @p 32.5 67 20.5 0 0"}) server.runCommand(c);
        context.waitTicks(200);
        for (float pitch : new float[] {10, -10, -21, -23, -24, -25, -27, -30}) {
            context.runOnClient(client -> { client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK); client.player.setYRot(0); client.player.setXRot(pitch); });
            context.waitTicks(15);
            String where = context.computeOnClient(client -> String.format(java.util.Locale.ROOT, "cam%+.2f",
                client.gameRenderer.mainCamera().position().y - 63));
            log("water pillar " + tag + " pitch " + pitch + ": camera " + where + " over the water");
            context.takeScreenshot(String.format(java.util.Locale.ROOT, "49-pillar-%s-p%+03d-%s", tag, (int) pitch, where));
        }
        context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        context.runOnClient(client -> client.options.renderDistance().set(5));
        for (String c : new String[] {"fill 0 55 0 63 66 63 air", "gamemode spectator @a"}) server.runCommand(c);
    }

    /** Writes the Complementary Unbound + Euphoria settings file (like its menu does) and reloads the shader pack. */
    private static void shaderOption(ClientGameTestContext context, String line) {
        context.runOnClient(client -> {
            try {
                var file = client.gameDirectory.toPath().resolve("shaderpacks/ComplementaryUnbound_r5.9.3 + EuphoriaPatches_1.10.5.txt");
                if (line.isEmpty()) java.nio.file.Files.deleteIfExists(file); else java.nio.file.Files.writeString(file, line + System.lineSeparator());
                var api = Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance").invoke(null);
                var config = api.getClass().getMethod("getConfig").invoke(api);
                config.getClass().getMethod("setShadersEnabledAndApply", boolean.class).invoke(config, false);
            } catch (Exception error) { throw new IllegalStateException(error); }
        });
        // Off and on in the same tick fails to build the pipeline; give it a moment between.
        context.waitTicks(40);
        context.runOnClient(client -> {
            try {
                var api = Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance").invoke(null);
                var config = api.getClass().getMethod("getConfig").invoke(api);
                config.getClass().getMethod("setShadersEnabledAndApply", boolean.class).invoke(config, true);
            } catch (Exception error) { throw new IllegalStateException(error); }
        });
        context.waitTicks(300);
    }

    private static void dhSetting(ClientGameTestContext context, String name, Object value) {
        context.runOnClient(client -> {
            try {
                var configs = Class.forName("com.seibel.distanthorizons.api.DhApi$Delayed").getField("configs").get(null);
                var graphics = configs.getClass().getMethod("graphics").invoke(configs);
                var setting = graphics.getClass().getMethod(name).invoke(graphics);
                for (var m : setting.getClass().getMethods()) if (m.getName().equals("setValue") && m.getParameterCount() == 1) { m.invoke(setting, value); break; }
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        });
    }

    private static void dhRendering(ClientGameTestContext context, boolean on) {
        context.runOnClient(client -> {
            try {
                var configs = Class.forName("com.seibel.distanthorizons.api.DhApi$Delayed").getField("configs").get(null);
                var graphics = configs.getClass().getMethod("graphics").invoke(configs);
                var value = graphics.getClass().getMethod("renderingEnabled").invoke(graphics);
                for (var m : value.getClass().getMethods()) if (m.getName().equals("setValue") && m.getParameterCount() == 1) { m.invoke(value, on); break; }
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        });
    }

    /** A drawn map and an explorer map (the kind found in camps and chests), held in first person and seen from the front. */
    private static void heldMaps(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        var server = world.getServer();
        server.runCommand("gamemode survival @a");
        for (String kind : new String[] {"drawn", "camp"}) {
            server.runCommand("clear @a");
            server.runOnServer(s -> {
                var player = s.getPlayerList().getPlayers().getFirst();
                var level = s.overworld();
                ItemStack map = net.minecraft.world.item.MapItem.create(level, player.getBlockX(), player.getBlockZ(), (byte) 0, true, true);
                net.minecraft.world.item.MapItem.renderBiomePreviewMap(level, map);
                // A camp map is its own 26.3 item (Dungeons and Taverns hands these out), with real map data.
                if (kind.equals("camp")) { var camp = new ItemStack(net.minecraft.world.item.Items.ABANDONED_CAMP_MAP); camp.set(net.minecraft.core.component.DataComponents.MAP_ID, map.get(net.minecraft.core.component.DataComponents.MAP_ID)); map = camp; }
                player.getInventory().setItem(0, map);
                player.getInventory().setSelectedSlot(0);
            });
            context.waitTicks(40);
            context.runOnClient(client -> { client.player.getInventory().setSelectedSlot(0); client.player.setXRot(35); client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); });
            context.waitTicks(10);
            context.takeScreenshot("45-map-" + kind + "-first-person");
            if (kind.equals("camp")) {
                boolean held = context.computeOnClient(client -> {
                    try {
                        var loader = field(Class.forName("dev.tr7zw.notenoughanimations.NEAnimationsLoader"), "INSTANCE");
                        for (var animation : (Iterable<?>) field(field(loader, "animationProvider"), "basicAnimations"))
                            if (animation.getClass().getSimpleName().equals("MapHoldingAnimation"))
                                return ((java.util.Set<?>) field(animation, "compatibleMaps")).contains(net.minecraft.world.item.Items.ABANDONED_CAMP_MAP);
                    } catch (ReflectiveOperationException error) { return false; }
                    return false;
                });
                check(held, "Not Enough Animations holds camp maps like filled maps");
            }
            context.runOnClient(client -> { client.player.setXRot(0); client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); });
            context.waitTicks(10);
            context.takeScreenshot("45-map-" + kind + "-front");
            context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
            context.waitTicks(10);
            context.takeScreenshot("45-map-" + kind + "-back");
        }
        context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        server.runCommand("clear @a");
    }

    /** Each rarity's fish hovered in the inventory, for the name and tooltip colors. */
    private static void fishTooltips(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        var server = world.getServer();
        server.runCommand("gamemode survival @a");
        String[] kinds = {"plain-cod", "plain-pondie", "plain-splashtail", "rare", "epic", "legendary", "mythic", "shiny"};
        for (String kind : kinds) {
            server.runCommand("clear @a");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(8));
            if (kind.equals("plain-cod")) server.runCommand("give @p cod");
            else if (kind.startsWith("plain-")) server.runCommand("give @p fishofthieves:" + kind.substring(6));
            else server.runCommand("execute as @p run legends give " + kind);
            context.waitTicks(5);
            server.runCommand("item replace entity @p container.20 from entity @p container.0");
            server.runCommand("item replace entity @p container.0 with air");
            context.waitTicks(5);
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitTicks(5);
            var pos = context.computeOnClient(client -> {
                var screen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) client.gui.screen();
                Slot target = null;
                for (var slot : screen.getMenu().slots)
                    if (slot.container instanceof net.minecraft.world.entity.player.Inventory && slot.getContainerSlot() == 20) target = slot;
                int left = (int) field(screen, "leftPos"), top = (int) field(screen, "topPos");
                double scale = client.getWindow().getGuiScale();
                return new double[] {(left + target.x + 8) * scale, (top + target.y + 8) * scale};
            });
            context.getInput().setCursorPos(pos[0], pos[1]);
            context.waitTicks(5);
            String name = context.computeOnClient(client -> {
                var stack = client.player.getInventory().getItem(20);
                var styled = stack.getStyledHoverName();
                var colors = new StringBuilder();
                styled.visit((style, text) -> { if (!text.isEmpty()) colors.append(text).append('=').append(style.getColor()).append(' '); return java.util.Optional.empty(); }, net.minecraft.network.chat.Style.EMPTY);
                var lines = new StringBuilder();
                for (var line : stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(client.level), client.player, net.minecraft.world.item.TooltipFlag.NORMAL))
                    line.visit((style, text) -> { if (!text.isBlank()) lines.append(" | ").append(text).append('=').append(style.getColor()); return java.util.Optional.empty(); }, net.minecraft.network.chat.Style.EMPTY);
                return holylois.boombox.FishData.rarity(stack) + ": " + colors + " lines" + lines;
            });
            log("fish tooltip " + kind + " -> " + name);
            // Hold-to-eat time grows with rarity; Mythics add a second per 20 kg (8 s at most); plain fish stay at 1.6 s.
            int ticks = context.computeOnClient(client -> client.player.getInventory().getItem(20).getUseDuration(client.player));
            double kg = context.computeOnClient(client -> holylois.boombox.FishData.kilograms(client.player.getInventory().getItem(20)));
            String rolled=context.computeOnClient(client->holylois.boombox.FishData.rarity(client.player.getInventory().getItem(20)));
            int expected = switch (rolled) { case "rare" -> 40; case "epic" -> 52; case "legendary" -> 64;
                case "mythic" -> (int) Math.round(Math.min(8, 3.2 + kg / 20) * 20); default -> 32; };
            check(ticks == expected, kind + " eats in " + ticks + " ticks (expected " + expected + ")");
            // The owner wants clean fish tooltips without color swatches or variant labels.
            String row = context.computeOnClient(client -> {
                var lines = client.player.getInventory().getItem(20).getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(client.level), client.player, net.minecraft.world.item.TooltipFlag.NORMAL);
                return lines.size() > 1 ? lines.get(1).getString() : "";
            });
            check(!name.contains("■"), kind + ": no fish color swatches (" + row + ")");
            if (kind.equals("plain-pondie")) check(!row.matches("[A-Z][a-z]+"),"pondie color label hidden (" + row + ")");
            context.takeScreenshot("46-fish-tooltip-" + kind);
            context.runOnClient(client -> client.gui.setScreen(null));
            context.waitTicks(3);
        }
        server.runCommand("clear @a");
    }

    /** A Fish of Thieves fish eaten in first person: the bite frames from the Holy Lois eating pack show up while eating. */
    private static void fishEatingFrames(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        var server = world.getServer();
        server.runCommand("gamemode survival @a");
        boolean packOn = context.computeOnClient(client -> client.getResourcePackRepository().getSelectedIds().stream().anyMatch(id -> id.contains("HolyLois-FishEating")));
        check(packOn, "the fish eating pack is enabled in the test client");
        for (String fish : new String[] {"pondie", "cooked_splashtail", "large-pondie"}) {
            server.runCommand("clear @a");
            if(fish.equals("large-pondie"))server.runOnServer(s->{
                var player=s.getPlayerList().getPlayers().getFirst();
                var stack=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("fishofthieves:pondie")));
                var data=new net.minecraft.nbt.CompoundTag();var trophy=new net.minecraft.nbt.CompoundTag();
                trophy.putString("rarity","mythic");trophy.putDouble("kg",120);data.put("holylois_fish",trophy);
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
                player.getInventory().setItem(0,stack);
            });
            else server.runCommand("give @p fishofthieves:" + fish);
            context.waitTicks(20);
            // HUD off (F1) so no catch card covers the hand.
            context.runOnClient(client -> { client.player.getInventory().setSelectedSlot(0); client.player.setXRot(20); client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); });
            context.getInput().pressKey(options -> options.keyToggleGui);
            server.runOnServer(s -> s.getPlayerList().getPlayers().forEach(p -> p.getFoodData().setFoodLevel(6)));
            context.getInput().holdKey(options -> options.keyUse);
            if(fish.equals("large-pondie")){
                context.waitTicks(8);context.takeScreenshot("47-eat-large-early-feedback");
                check(context.computeOnClient(client->client.player.isUsingItem()&&client.player.getUseItemRemainingTicks()>128),"large fish starts eating while most of its meal remains");
                context.waitTicks(100);
            }else context.waitTicks(22);
            boolean eating = context.computeOnClient(client -> client.player.isUsingItem());
            String texture=context.computeOnClient(client->{
                var state=new net.minecraft.client.renderer.item.ItemStackRenderState();
                client.getItemModelResolver().updateForLiving(state,client.player.getUseItem(),net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,client.player);
                return state.pickParticleMaterial(client.player.getRandom()).sprite().contents().name().toString();
            });
            log("eating model "+fish+": "+texture);
            context.takeScreenshot("47-eat-" + fish);
            context.runOnClient(client->{
                var stack=client.player.getUseItem();
                var prop=new net.minecraft.client.renderer.item.properties.numeric.UseDuration(false);
                log("eating progress "+fish+": total="+stack.getUseDuration(client.player)+" remaining="+client.player.getUseItemRemainingTicks()+" model="+prop.get(stack,client.level,client.player,0));
                try {
                    var resource=client.getResourceManager().getResource(Identifier.parse("fishofthieves:items/"+(fish.equals("large-pondie")?"pondie":fish)+".json")).orElseThrow();
                    log("winning fish model source: "+resource.sourcePackId());
                }catch(Exception error){throw new IllegalStateException(error);}
            });
            check(texture.contains("holylois:item/eating/"),fish+" resolves a real bite-frame model while eating: "+texture);
            context.getInput().releaseKey(options -> options.keyUse);
            context.getInput().pressKey(options -> options.keyToggleGui);
            context.waitTicks(10);
            check(eating, fish + " is being eaten when the frame is captured");
        }
        server.runCommand("clear @a");
    }

    private static void armAnimations(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        var server = world.getServer();
        server.runCommand("gamemode survival @a");
        String[][] cases = {{"filled_map", "map"}, {"cooked_beef", "eat"}, {"lantern", "lantern"}};
        for (var c : cases) {
            server.runCommand("clear @a");
            server.runCommand("give @a " + c[0] + " 8");
            context.waitTicks(c[1].equals("eat") ? 40 : 10);
            context.runOnClient(client -> { client.player.getInventory().setSelectedSlot(0); client.player.setXRot(25); client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); });
            boolean eat = c[1].equals("eat");
            // Peaceful refills food, so the player is made hungry right before each bite.
            if (eat) { server.runOnServer(s -> s.getPlayerList().getPlayers().forEach(p -> p.getFoodData().setFoodLevel(6))); context.getInput().holdKey(options -> options.keyUse); }
            context.waitTicks(eat ? 12 : 6);
            context.takeScreenshot("40-" + c[1] + "-first-person");
            if (eat) { context.getInput().releaseKey(options -> options.keyUse); context.waitTicks(4); }
            context.runOnClient(client -> { client.player.setXRot(0); client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); });
            if (eat) { server.runOnServer(s -> s.getPlayerList().getPlayers().forEach(p -> p.getFoodData().setFoodLevel(6))); context.getInput().holdKey(options -> options.keyUse); }
            context.waitTicks(12);
            context.takeScreenshot("41-" + c[1] + "-front");
            if (eat) context.getInput().releaseKey(options -> options.keyUse);
        }
        context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        // Our smoothing went into Not Enough Animations' lantern and hold-up code (the mixins are optional, so check here).
        for (var owner : new String[] {"dev.tr7zw.notenoughanimations.logic.HeldItemHandler", "dev.tr7zw.notenoughanimations.animations.hands.LookAtItemAnimation"}) {
            boolean merged = java.util.Arrays.stream(Class.forName(owner).getDeclaredMethods()).anyMatch(m -> m.getName().contains("holyLoisSmooth"));
            check(merged, "smoothing merged into " + owner);
        }
        server.runCommand("clear @a");
        server.runCommand("effect clear @a");
        server.runCommand("effect give @a saturation 3 20 true");
        server.runCommand("effect give @a instant_health 1 10 true");
        context.waitTicks(40);
    }

    /** Typing a password and pressing Enter sends it, like clicking Log in (the field is cleared once sent). */
    private static void enterSubmitsLogin(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        // Like a player: click into the box first (mouse focus), type, Enter. Login, then register (Enter in the confirm box).
        for (int mode : new int[] {1, 2}) {
            var screen = context.computeOnClient(client -> { var s = new holylois.auth.HolyLoisAuthScreen(new holylois.auth.AuthStatus(mode, 6)); client.gui.setScreen(s); return s; });
            context.waitTicks(5);
            clickField(context, screen, "password");
            context.getInput().typeChars("secret123");
            if (mode == 2) { clickField(context, screen, "confirm"); context.getInput().typeChars("secret123"); }
            context.waitTicks(2);
            String typed = context.computeOnClient(client -> passwordOf(client.gui.screen()));
            String focus = context.computeOnClient(client -> String.valueOf(client.gui.screen().getFocused()));
            context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN);
            context.waitTicks(3);
            boolean same = context.computeOnClient(client -> client.gui.screen() == screen);
            String notice = String.valueOf(field(screen, "notice"));
            log("auth form " + mode + ": typed=" + typed.length() + " focus=" + focus + " same=" + same + " notice=" + notice);
            context.runOnClient(client -> client.gui.setScreen(null));
            check(typed.length() == 9, "the password box takes typed text (" + typed.length() + ")");
            check(same && notice.startsWith("Checking"), "Enter in a focused box sent form " + mode + " (notice: " + notice + ")");
        }
        // Esc opens the leave prompt (26.3 scancode 41, not GLFW 256).
        context.runOnClient(client -> client.gui.setScreen(new holylois.auth.HolyLoisAuthScreen(new holylois.auth.AuthStatus(1, 6))));
        context.waitTicks(3);
        context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);
        context.waitTicks(2);
        boolean quitting = context.computeOnClient(client -> client.gui.screen() instanceof holylois.auth.HolyLoisAuthScreen s && (boolean) field(s, "quitting"));
        context.runOnClient(client -> client.gui.setScreen(null));
        check(quitting, "Esc on the login form opens the leave prompt");
    }

    private static void clickField(ClientGameTestContext context, Object screen, String name) {
        var box = (net.minecraft.client.gui.components.EditBox) field(screen, name);
        double scale = context.computeOnClient(client -> client.getWindow().getGuiScale());
        context.getInput().setCursorPos((box.getX() + 20) * scale, (box.getY() + 4) * scale);
        context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(1);
    }

    private static Object field(Object owner, String name) {
        // A Class reads a static field.
        for (Class<?> type = owner instanceof Class<?> c ? c : owner.getClass(); type != null; type = type.getSuperclass()) {
            try { var f = type.getDeclaredField(name); f.setAccessible(true); return f.get(owner instanceof Class<?> ? null : owner); }
            catch (NoSuchFieldException next) { continue; }
            catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        }
        throw new IllegalStateException("no field " + name + " on " + owner);
    }

    private static String passwordOf(Object screen) {
        try {
            var field = screen.getClass().getDeclaredField("password"); field.setAccessible(true);
            var box = (net.minecraft.client.gui.components.EditBox) field.get(screen);
            return box == null ? "" : box.getValue();
        } catch (ReflectiveOperationException error) { return "no field on " + screen; }
    }

    /** The 1.9.0 boombox model from the front, placed like a player would, then turned to face the camera. */
    private static void boomboxModel(ClientGameTestContext context, TestSingleplayerContext world) {
        BlockPos target = context.computeOnClient(client -> client.player.blockPosition().offset(-2, 0, 0));
        world.getServer().runOnServer(s -> {
            place(s, target);
            var level = s.overworld();
            level.setBlockAndUpdate(target, level.getBlockState(target).setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST));
        });
        context.getInput().lookAt(target);
        context.waitTicks(20);
        context.takeScreenshot("13-boombox-model");
        // Held: the flat icon in the hotbar, the 3D model carried by its handle in both views.
        world.getServer().runCommand("clear @a");
        world.getServer().runCommand("give @a holylois:boombox");
        // With FirstPerson your own body carries it: look down at the hand.
        context.runOnClient(client -> client.player.setXRot(65));
        context.waitTicks(20);
        context.takeScreenshot("14-boombox-held-first-person");
        // Side view on an armor stand three blocks ahead: the speakers should face out, away from the leg.
        var ahead = context.computeOnClient(client -> client.player.position().add(client.player.getLookAngle().multiply(1, 0, 1).normalize().scale(3)));
        float facing = context.computeOnClient(client -> client.player.getYRot() + 90);
        world.getServer().runCommand(String.format(java.util.Locale.ROOT,
            "summon armor_stand %.2f %.2f %.2f {ShowArms:1b,NoBasePlate:1b,Rotation:[%.1ff,0f],equipment:{mainhand:{id:\"holylois:boombox\",count:1}}}",
            ahead.x, ahead.y, ahead.z, facing));
        context.runOnClient(client -> client.player.setXRot(10));
        context.waitTicks(20);
        context.takeScreenshot("14-boombox-held-side");
        world.getServer().runCommand("kill @e[type=armor_stand]");
        context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
        context.waitTicks(20);
        context.takeScreenshot("15-boombox-held-third-person");
        // Walk, stop and turn: the arm stays at the hip and the box swings from its handle, then settles.
        context.getInput().holdKey(options -> options.keyUp);
        context.waitTicks(3);
        float tiltStart = context.computeOnClient(client -> holylois.boombox.HeldSwing.tilt(client.player.getId()));
        context.waitTicks(7);
        context.takeScreenshot("15-boombox-walk");
        context.getInput().releaseKey(options -> options.keyUp);
        context.waitTicks(3);
        float afterStop = context.computeOnClient(client -> holylois.boombox.HeldSwing.side(client.player.getId()));
        float tiltStop = context.computeOnClient(client -> holylois.boombox.HeldSwing.tilt(client.player.getId()));
        // A little front/back (owner 2026-10-11): it lags back as you set off and swings forward when you stop.
        check(tiltStart < -1 && tiltStop > 1, "the carried boombox lags back on setting off and swings forward on stopping (" + tiltStart + ", " + tiltStop + ")");
        context.takeScreenshot("15-boombox-stop");
        context.runOnClient(client -> client.player.setYRot(client.player.getYRot() + 70));
        context.waitTicks(4);
        float afterTurn = context.computeOnClient(client -> holylois.boombox.HeldSwing.side(client.player.getId()));
        context.takeScreenshot("15-boombox-turn");
        log("boombox swing: stop side " + afterStop + " tilt " + tiltStop + ", turn side " + afterTurn);
        check(Math.abs(tiltStop) > 2 && Math.abs(afterTurn) > 2, "the carried boombox swings when you stop and turn (" + tiltStop + ", " + afterTurn + ")");
        context.runOnClient(client -> client.player.setYRot(client.player.getYRot() - 70));
        context.waitTicks(40);
        context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        world.getServer().runCommand("clear @a");
        // Mannequins 3 blocks ahead: one faces the camera, one shows its side (hand on the handle, speakers out).
        var look = context.computeOnClient(client -> client.player.getLookAngle().multiply(1, 0, 1).normalize());
        var base = context.computeOnClient(client -> client.player.position());
        var right = new net.minecraft.world.phys.Vec3(-look.z, 0, look.x);
        float faceCamera = context.computeOnClient(client -> client.player.getYRot() + 180);
        var front = base.add(look.scale(3)).add(right.scale(-1));
        var side = base.add(look.scale(3)).add(right.scale(1));
        world.getServer().runCommand(String.format(java.util.Locale.ROOT, "summon mannequin %.2f %.2f %.2f {Tags:[\"hlbox\"],Rotation:[%.1ff,0f],equipment:{mainhand:{id:\"holylois:boombox\",count:1}}}", front.x, front.y, front.z, faceCamera));
        world.getServer().runCommand(String.format(java.util.Locale.ROOT, "summon mannequin %.2f %.2f %.2f {Tags:[\"hlbox\"],Rotation:[%.1ff,0f],equipment:{mainhand:{id:\"holylois:boombox\",count:1}}}", side.x, side.y, side.z, faceCamera + 90));
        context.runOnClient(client -> client.player.setXRot(15));
        context.waitTicks(20);
        context.takeScreenshot("16-boombox-held-mannequins");
        world.getServer().runCommand("kill @e[tag=hlbox]");
    }

    /**
     * R over a fillet in the inventory shows its recipe and never sorts: nothing moves, nothing sticks to the cursor, and the
     * client and the server agree on every slot afterwards (owner round 4: a sort plus a ghost slice on the cursor).
     */
    private static void rOverFillet(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        var server = world.getServer();
        // Earlier screen tests can leave a native container open behind a client-only screen.
        log("R fixture previous menu: client="+context.computeOnClient(client->client.player.containerMenu.containerId)
            +" server="+server.computeOnServer(s->s.getPlayerList().getPlayers().getFirst().containerMenu.containerId));
        context.runOnClient(client->client.player.closeContainer());
        server.runOnServer(s->s.getPlayerList().getPlayers().getFirst().closeContainer());
        context.waitTicks(5);
        server.runCommand("clear @a");
        server.runOnServer(s -> {
            var player = s.getPlayerList().getPlayers().getFirst();
            var fish = new ItemStack(net.minecraft.world.item.Items.COD);
            var tag = new net.minecraft.nbt.CompoundTag(); var inner = new net.minecraft.nbt.CompoundTag();
            inner.putString("rarity", "legendary"); inner.putString("species", "minecraft:cod"); inner.putDouble("kg", 25);
            tag.put("holylois_fish", inner);
            fish.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            var slice = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("farmersdelight:cod_slice")), 5);
            holylois.boombox.Legends.fillet(fish, slice);
            player.getInventory().setItem(20, slice);
            player.getInventory().setItem(9, new ItemStack(net.minecraft.world.item.Items.DIRT, 3));
            player.getInventory().setItem(30, new ItemStack(net.minecraft.world.item.Items.STONE, 7));
        });
        context.waitTicks(5);
        context.getInput().pressKey(options -> options.keyInventory);
        context.waitForScreen(InventoryScreen.class);
        // Hover the slice (inventory slot 20 is menu slot 20 in the player's inventory screen).
        var pos = context.computeOnClient(client -> {
            var screen = (InventoryScreen) client.gui.screen();
            var slot = screen.getMenu().getSlot(20);
            int left = (int) field(screen, "leftPos"), top = (int) field(screen, "topPos");
            double scale = client.getWindow().getGuiScale();
            return new double[] {(left + slot.x + 8) * scale, (top + slot.y + 8) * scale};
        });
        context.getInput().setCursorPos(pos[0], pos[1]);
        context.waitTicks(3);
        String hovered = context.computeOnClient(client -> String.valueOf(((holylois.boombox.mixins.ContainerHoverAccessor) client.gui.screen()).holyLoisHoveredSlot()));
        context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_R);
        context.waitTicks(10);
        context.takeScreenshot("35-r-over-fillet");
        String screenAfter = context.computeOnClient(client -> String.valueOf(client.gui.screen()));
        String clientState = context.computeOnClient(client -> inventoryState(client.player.containerMenu.getCarried(), client.player.getInventory()));
        String serverState = server.computeOnServer(s -> { var p = s.getPlayerList().getPlayers().getFirst(); return inventoryState(p.containerMenu.getCarried(), p.getInventory()); });
        log("R over fillet: hovered=" + hovered + " screen=" + screenAfter + "\n client " + clientState + "\n server " + serverState);
        // R over an empty slot sorts (Inventory Profiles Next), and still nothing ends up on the cursor.
        context.runOnClient(client -> { if (!(client.gui.screen() instanceof InventoryScreen)) client.gui.setScreen(new InventoryScreen(client.player)); });
        context.waitTicks(3);
        var empty = context.computeOnClient(client -> {
            var screen = (InventoryScreen) client.gui.screen();
            var slot = screen.getMenu().getSlot(25);
            int left = (int) field(screen, "leftPos"), top = (int) field(screen, "topPos");
            double scale = client.getWindow().getGuiScale();
            return new double[] {(left + slot.x + 8) * scale, (top + slot.y + 8) * scale};
        });
        context.getInput().setCursorPos(empty[0], empty[1]);
        context.waitTicks(3);
        context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_R);
        context.waitTicks(20);
        String clientSorted = context.computeOnClient(client -> inventoryState(client.player.containerMenu.getCarried(), client.player.getInventory()));
        String serverSorted = server.computeOnServer(s -> { var p = s.getPlayerList().getPlayers().getFirst(); return inventoryState(p.containerMenu.getCarried(), p.getInventory()); });
        // IPN sends a queued click sequence. A loaded full-suite run can still be flushing it after twenty ticks.
        for(int settle=0;!clientSorted.equals(serverSorted)&&settle<16;settle++){
            context.waitTicks(5);
            clientSorted=context.computeOnClient(client->inventoryState(client.player.containerMenu.getCarried(),client.player.getInventory()));
            serverSorted=server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return inventoryState(p.containerMenu.getCarried(),p.getInventory());});
        }
        log("R over empty slot:\n client " + clientSorted + "\n server " + serverSorted);
        check(clientSorted.equals(serverSorted) && serverSorted.startsWith("cursor=empty"), "sorting leaves nothing on the cursor and client and server agree");
        context.runOnClient(client -> client.gui.setScreen(null));
        context.waitTicks(5);
        check(clientState.equals(serverState), "client and server agree after R over a fillet");
        check(serverState.contains("20=5 farmersdelight:cod_slice") && serverState.startsWith("cursor=empty"), "R over a fillet moved nothing (" + serverState + ")");
        // Owner 2026-10-10 (again): R on the empty slot the sort is about to fill must sort only, never show that item's recipe.
        for (boolean chest : new boolean[] {false, true}) rSortFillsHoveredSlot(context, world, chest);
    }

    private static void rSortFillsHoveredSlot(ClientGameTestContext context, TestSingleplayerContext world, boolean chest) throws Exception {
        var server = world.getServer();
        server.runCommand("clear @a");
        BlockPos base = context.computeOnClient(client -> client.player.blockPosition());
        BlockPos box = base.offset(0, 0, 3);
        if (chest) {
            server.runCommand("setblock " + box.getX() + " " + box.getY() + " " + box.getZ() + " chest{Items:[{Slot:20b,id:\"minecraft:stone\",count:7},{Slot:24b,id:\"minecraft:dirt\",count:3}]}");
            context.waitTicks(10);
            context.getInput().lookAt(box);
            context.waitTicks(5);
            context.getInput().pressKey(options -> options.keyUse);
            context.waitForScreen(ContainerScreen.class);
        } else {
            server.runOnServer(s -> {
                var player = s.getPlayerList().getPlayers().getFirst();
                player.getInventory().setItem(30, new ItemStack(net.minecraft.world.item.Items.STONE, 7));
                player.getInventory().setItem(34, new ItemStack(net.minecraft.world.item.Items.DIRT, 3));
            });
            context.waitTicks(5);
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(InventoryScreen.class);
        }
        context.waitTicks(5);
        // The first slot of the sorted area: empty now, the first sorted stack lands right under the cursor.
        int index = chest ? 0 : 9;
        var at = context.computeOnClient(client -> {
            var screen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) client.gui.screen();
            var slot = screen.getMenu().getSlot(index);
            int left = (int) field(screen, "leftPos"), top = (int) field(screen, "topPos");
            double scale = client.getWindow().getGuiScale();
            return new double[] {(left + slot.x + 8) * scale, (top + slot.y + 8) * scale};
        });
        context.getInput().setCursorPos(at[0], at[1]);
        context.waitTicks(3);
        // A natural press: held for about half a second, like a finger, not an instant tap.
        context.getInput().holdKey(com.mojang.blaze3d.platform.InputConstants.KEY_R);
        context.waitTicks(12);
        context.getInput().releaseKey(com.mojang.blaze3d.platform.InputConstants.KEY_R);
        context.waitTicks(30);
        String screen = context.computeOnClient(client -> client.gui.screen() == null ? "none" : client.gui.screen().getClass().getName());
        boolean filled = context.computeOnClient(client -> client.player.containerMenu.getSlot(index).hasItem());
        context.takeScreenshot("35b-r-sort-" + (chest ? "chest" : "inventory"));
        log("R sort into the hovered slot (" + (chest ? "chest" : "inventory") + "): slot filled " + filled + ", screen " + screen);
        check(filled && (chest ? screen.endsWith("ContainerScreen") : screen.endsWith("InventoryScreen")),
            "R on an empty slot sorts into it without opening a recipe (" + (chest ? "chest" : "inventory") + ": filled " + filled + ", " + screen + ")");
        context.runOnClient(client -> client.gui.setScreen(null));
        context.waitTicks(5);
        if (chest) server.runCommand("setblock " + box.getX() + " " + box.getY() + " " + box.getZ() + " air");
    }

    /**
     * Owner round 5 dupe: torch in the off hand, a heavy fish in the inventory, spam F over the fish. The fish must never reach
     * the off hand, there is always exactly one fish, and the client and the server agree after every press.
     */
    private static void offhandSwapSpam(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        swapSpam(context, world, false);
        swapSpam(context, world, true);
        world.getServer().runCommand("gamemode survival @a");
    }

    /**
     * Owner 2026-10-11: picking a fish up and putting it back in the same slot sometimes left a phantom fish there (the client
     * showed one the server did not have). Small and heavy trophies, survival and creative, quick and slow clicks.
     */
    private static void fishSameSlot(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        var server = world.getServer();
        var results = new java.util.ArrayList<String>();
        for (String mode : new String[] {"survival", "creative"})
            for (String rarity : new String[] {"rare", "legendary"}) {
                server.runCommand("gamemode " + mode + " @a");
                server.runCommand("clear @a");
                // Hold slot 9 so Inventory Profiles Next does not refill the hand from the moved fish.
                context.runOnClient(client -> client.player.getInventory().setSelectedSlot(8));
                server.runCommand("execute as @p run legends give " + rarity);
                context.waitTicks(5);
                // Commands move it like gameplay does, so the client is told (a direct server-side move is not synced in creative).
                server.runCommand("item replace entity @p container.20 from entity @p container.0");
                server.runCommand("item replace entity @p container.0 with air");
                context.waitTicks(5);
                context.getInput().pressKey(options -> options.keyInventory);
                context.waitTicks(5);
                if (mode.equals("creative")) context.runOnClient(client -> {
                    var screen = client.gui.screen();
                    for (var tab : BuiltInRegistries.CREATIVE_MODE_TAB)
                        if (tab.getType() == net.minecraft.world.item.CreativeModeTab.Type.INVENTORY) {
                            var select = screen.getClass().getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
                            select.setAccessible(true); select.invoke(screen, tab);
                        }
                });
                context.waitTicks(3);
                var pos = context.computeOnClient(client -> {
                    var screen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) client.gui.screen();
                    Slot target = null;
                    for (var slot : screen.getMenu().slots)
                        if (slot.container instanceof net.minecraft.world.entity.player.Inventory && slot.getContainerSlot() == 20) target = slot;
                    int left = (int) field(screen, "leftPos"), top = (int) field(screen, "topPos");
                    double scale = client.getWindow().getGuiScale();
                    return new double[] {(left + target.x + 8) * scale, (top + target.y + 8) * scale};
                });
                context.getInput().setCursorPos(pos[0], pos[1]);
                context.waitTicks(2);
                String startC = context.computeOnClient(client -> slotState(client.player) + " fishAt " + fishAt(client.player));
                String startS = server.computeOnServer(s -> { var p = s.getPlayerList().getPlayers().getFirst(); return slotState(p) + " fishAt " + fishAt(p); });
                log("same-slot start " + mode + " " + rarity + ": client " + startC + " | server " + startS);
                int bad = 0;
                for (int round = 0; round < 8; round++) {
                    context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
                    context.waitTicks(round % 2 == 0 ? 1 : 4);
                    context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
                    context.waitTicks(round % 3 == 0 ? 1 : 5);
                    String c = context.computeOnClient(client -> slotState(client.player));
                    String sv = server.computeOnServer(s -> slotState(s.getPlayerList().getPlayers().getFirst()));
                    // Back in its slot after every pick-and-place, never left on the cursor (the "phantom fish").
                    if (!c.startsWith("slot cod") || !c.endsWith("cursor -")) { bad++; log("same-slot not placed " + mode + " " + rarity + " round " + round + ": client " + c); }
                    else if (!c.equals(sv)) { bad++; log("same-slot mismatch " + mode + " " + rarity + " round " + round + ": client " + c + " | server " + sv); }
                }
                context.runOnClient(client -> client.gui.setScreen(null));
                context.waitTicks(5);
                results.add(mode + " " + rarity + ": " + bad + " mismatches");
            }
        server.runCommand("gamemode survival @a");
        server.runCommand("clear @a");
        log("fish same slot: " + results);
        check(results.stream().allMatch(r -> r.endsWith(": 0 mismatches")), "a fish put back in its slot never leaves a phantom (" + results + ")");
    }

    private static String fishAt(net.minecraft.world.entity.player.Player player) {
        var at = new java.util.ArrayList<Integer>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (!player.getInventory().getItem(i).isEmpty()) at.add(i);
        return at.toString();
    }

    /** Slot 20 and the cursor as item id, count and fish weight label. */
    private static String slotState(net.minecraft.world.entity.player.Player player) {
        java.util.function.Function<ItemStack, String> d = st -> st.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(st.getItem()).getPath() + "x" + st.getCount() + "@" + holylois.boombox.FishData.label(st);
        var carried = player.containerMenu.getCarried();
        // The creative screen carries its cursor item in its own picker menu, not the player's inventory menu.
        if (player.level().isClientSide() && net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen && carried.isEmpty())
            carried = screen.getMenu().getCarried();
        return "slot " + d.apply(player.getInventory().getItem(20)) + " cursor " + d.apply(carried);
    }

    private static void swapSpam(ClientGameTestContext context, TestSingleplayerContext world, boolean creative) throws Exception {
        var server = world.getServer();
        server.runCommand("gamemode " + (creative ? "creative" : "survival") + " @a");
        server.runCommand("clear @a");
        server.runCommand("execute as @p run legends give legendary");
        context.waitTicks(5);
        server.runOnServer(s -> {
            var player = s.getPlayerList().getPlayers().getFirst();
            int from = -1;
            for (int i = 0; i < 36; i++) if (holylois.boombox.FishData.twoHanded(player.getInventory().getItem(i))) from = i;
            player.getInventory().setItem(20, player.getInventory().removeItemNoUpdate(from));
            player.getInventory().setSelectedSlot(4);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(net.minecraft.world.item.Items.TORCH, 3));
        });
        context.waitTicks(5);
        context.getInput().pressKey(options -> options.keyInventory);
        context.waitTicks(5);
        if (creative) context.runOnClient(client -> {
            var screen = client.gui.screen();
            for (var tab : BuiltInRegistries.CREATIVE_MODE_TAB)
                if (tab.getType() == net.minecraft.world.item.CreativeModeTab.Type.INVENTORY) {
                    var select = screen.getClass().getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
                    select.setAccessible(true); select.invoke(screen, tab);
                }
        });
        context.waitTicks(3);
        var pos = context.computeOnClient(client -> {
            var screen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) client.gui.screen();
            Slot target = null;
            for (var slot : screen.getMenu().slots)
                if (slot.container instanceof net.minecraft.world.entity.player.Inventory && slot.getContainerSlot() == 20) target = slot;
            int left = (int) field(screen, "leftPos"), top = (int) field(screen, "topPos");
            double scale = client.getWindow().getGuiScale();
            return new double[] {(left + target.x + 8) * scale, (top + target.y + 8) * scale};
        });
        context.getInput().setCursorPos(pos[0], pos[1]);
        context.waitTicks(2);
        for (int press = 0; press < 20; press++) {
            context.getInput().pressKey(options -> options.keySwapOffhand);
            context.waitTicks(press % 3 == 0 ? 1 : 3);
        }
        context.waitTicks(10);
        String clientState = context.computeOnClient(client -> handsState(client.player));
        String serverState = server.computeOnServer(s -> handsState(s.getPlayerList().getPlayers().getFirst()));
        String mode = creative ? "creative" : "survival";
        log("F spam over a heavy fish (" + mode + "): client " + clientState + " | server " + serverState);
        context.runOnClient(client -> client.gui.setScreen(null));
        context.waitTicks(3);
        check(serverState.contains("fish=1 ") && serverState.contains("offhandFish=false"), mode + ": exactly one fish, never in the off hand (" + serverState + ")");
        check(serverState.contains("torches=3"), mode + ": the torch stack is untouched (" + serverState + ")");
        check(clientState.equals(serverState), mode + ": client and server agree after F spam");
    }

    private static String handsState(net.minecraft.world.entity.player.Player player) {
        int fish = 0, torches = 0;
        var all = new java.util.ArrayList<ItemStack>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) all.add(player.getInventory().getItem(i));
        all.add(player.containerMenu.getCarried());
        for (var stack : all) {
            if (holylois.boombox.FishData.twoHanded(stack)) fish += stack.getCount();
            if (stack.is(net.minecraft.world.item.Items.TORCH)) torches += stack.getCount();
        }
        return "fish=" + fish + " torches=" + torches + " offhandFish=" + holylois.boombox.FishData.twoHanded(player.getOffhandItem());
    }

    private static String inventoryState(ItemStack carried, net.minecraft.world.entity.player.Inventory inventory) {
        var out = new StringBuilder("cursor=" + (carried.isEmpty() ? "empty" : carried.getCount() + " " + BuiltInRegistries.ITEM.getKey(carried.getItem())));
        for (int i = 0; i < 36; i++) {
            var stack = inventory.getItem(i);
            if (!stack.isEmpty()) out.append(' ').append(i).append('=').append(stack.getCount()).append(' ').append(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        return out.toString();
    }

    /** Close-ups of a mannequin carrying the boombox: from the front and from its side (handle in the fist, box at the hip). */
    private static void boomboxHand(ClientGameTestContext context, TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("clear @a");
        server.runCommand("kill @e[type=mannequin]");
        // Zoomed in on the fist (narrow field of view, no HUD) so a pixel of offset shows.
        int fov = context.computeOnClient(client -> client.options.fov().get());
        context.runOnClient(client -> { client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); client.player.setXRot(0);
            client.options.fov().set(40); if (!client.gui.hud.isHidden()) client.gui.hud.toggle(); });
        var look = context.computeOnClient(client -> client.player.getLookAngle().multiply(1, 0, 1).normalize());
        var spot = context.computeOnClient(client -> client.player.position()).add(look.scale(1.3)).add(0, 0.95, 0);
        float faceCamera = context.computeOnClient(client -> client.player.getYRot() + 180);
        String[] names = {"front", "side", "back", "inside"};
        float[] turns = {0, 90, 180, 270};
        for (int i = 0; i < 4; i++) {
            server.runCommand(String.format(java.util.Locale.ROOT, "summon mannequin %.2f %.2f %.2f {NoGravity:1b,Tags:[\"hlhand\"],Rotation:[%.1ff,0f],equipment:{mainhand:{id:\"holylois:boombox\",count:1}}}", spot.x, spot.y, spot.z, faceCamera + turns[i]));
            context.waitTicks(25);
            context.takeScreenshot("17-boombox-hand-" + names[i]);
            if (i == 3) {
                // Music reaching a held boombox (voice chat entity sound): the cones push out like on the placed one.
                java.util.UUID holder = context.computeOnClient(client -> { for (var e : client.level.entitiesForRendering())
                    if (e.getClass().getSimpleName().contains("Mannequin")) return e.getUUID(); return null; });
                for (int t = 0; t <= 64; t++) { int frame = t; context.runOnClient(client -> holylois.boombox.BoomboxPulse.heardHeld(holder, music(frame))); context.waitTicks(1); }
                context.takeScreenshot("17-boombox-hand-playing");
                float pushed = context.computeOnClient(client -> ((java.util.Map<?, Float>) field(holylois.boombox.BoomboxPulse.class, "heldShown")).getOrDefault(holder, 0f));
                log("held boombox cones pushed out by " + pushed);
                check(pushed > 0.1f, "the held boombox cones move with the music (" + pushed + ")");
            }
            server.runCommand("tp @e[tag=hlhand] ~ -300 ~");
            server.runCommand("kill @e[tag=hlhand]");
        }
        context.runOnClient(client -> { client.options.fov().set(fov); if (client.gui.hud.isHidden()) client.gui.hud.toggle(); });
    }

    /** A lantern in hand swings on the boombox pendulum (HeldSwing through Not Enough Animations' 3D lantern). */
    private static void lanternSwing(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        var server = world.getServer();
        server.runCommand("clear @a");
        server.runCommand("item replace entity @a weapon.mainhand with lantern");
        context.runOnClient(client -> { client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); client.player.setXRot(10); });
        context.waitTicks(20);
        context.takeScreenshot("42-lantern-still");
        context.getInput().holdKey(options -> options.keyUp);
        context.waitTicks(10);
        context.takeScreenshot("42-lantern-walk");
        context.getInput().releaseKey(options -> options.keyUp);
        context.waitTicks(3);
        float tilt = context.computeOnClient(client -> holylois.boombox.HeldSwing.tilt(client.player.getId()));
        context.takeScreenshot("42-lantern-stop");
        context.runOnClient(client -> client.player.setYRot(client.player.getYRot() + 70));
        context.waitTicks(4);
        float side = context.computeOnClient(client -> holylois.boombox.HeldSwing.side(client.player.getId()));
        context.takeScreenshot("42-lantern-turn");
        log("lantern swing: stop tilt " + tilt + ", turn side " + side);
        check(Math.abs(tilt) > 2 && Math.abs(side) > 2, "a held lantern swings when you stop and turn (" + tilt + ", " + side + ")");
        boolean merged = java.util.Arrays.stream(Class.forName("dev.tr7zw.notenoughanimations.logic.HeldItemHandler").getDeclaredMethods())
            .anyMatch(m -> m.getName().contains("holyLoisSmoothSwing"));
        check(merged, "the lantern swing is ours inside Not Enough Animations");
        context.runOnClient(client -> client.player.setYRot(client.player.getYRot() - 70));
        context.waitTicks(40);
        context.takeScreenshot("42-lantern-settled");
        // First person: the lantern hangs from your own hand (FirstPerson body), looking ahead and down.
        context.runOnClient(client -> { client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); client.player.setXRot(20); });
        context.waitTicks(10);
        context.takeScreenshot("42-lantern-first-person");
        context.runOnClient(client -> client.player.setXRot(60));
        context.waitTicks(10);
        context.takeScreenshot("42-lantern-first-person-down");
        float rest = context.computeOnClient(client -> Math.abs(holylois.boombox.HeldSwing.side(client.player.getId())) + Math.abs(holylois.boombox.HeldSwing.tilt(client.player.getId())));
        check(rest < 3, "the lantern settles when you stand still (" + rest + ")");
        context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        server.runCommand("clear @a");
    }

    /** The Holy Lootbox: the website's gold block with the engraved logo, in the hotbar, in hand (F5) and on the ground. */
    private static void holyLootbox(ClientGameTestContext context, TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("clear @a");
        server.runCommand("item replace entity @a weapon.mainhand with holylois:holy_lootbox");
        context.runOnClient(client -> { client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); client.player.setXRot(15); });
        context.waitTicks(20);
        context.takeScreenshot("43-lootbox-held");
        // The tooltip says what it is and which key opens it (the player's own Use binding).
        String tip = context.computeOnClient(client -> String.join(" | ", client.player.getMainHandItem()
            .getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(client.level), client.player,
                net.minecraft.world.item.TooltipFlag.NORMAL).stream().map(net.minecraft.network.chat.Component::getString).toList()));
        log("lootbox tooltip: " + tip);
        check(tip.contains("Daily gift from Holy Lois") && tip.contains(" to open"), "the lootbox tooltip says what it is and which key opens it");
        context.runOnClient(client -> { client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); client.player.setXRot(30); });
        context.waitTicks(10);
        context.takeScreenshot("43-lootbox-first-person");
        var ahead = context.computeOnClient(client -> client.player.position().add(client.player.getLookAngle().multiply(1, 0, 1).normalize().scale(1.6)));
        server.runCommand(String.format(java.util.Locale.ROOT, "summon item %.2f %.2f %.2f {Item:{id:\"holylois:holy_lootbox\",count:1},PickupDelay:32767}", ahead.x, ahead.y, ahead.z));
        context.runOnClient(client -> client.player.setXRot(50));
        context.waitTicks(30);
        context.takeScreenshot("43-lootbox-ground");
        server.runCommand("kill @e[type=item]");
        server.runCommand("clear @a");
        context.runOnClient(client -> client.player.setXRot(0));
    }

    /** 3D Armor (owner round 5): worn armor in 3D on a mannequin (diamond with a trim, dyed leather) and on you in first person. */
    private static void armor3d(ClientGameTestContext context, TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("clear @a");
        server.runCommand("kill @e[type=mannequin]");
        var look = context.computeOnClient(client -> client.player.getLookAngle().multiply(1, 0, 1).normalize());
        var base = context.computeOnClient(client -> client.player.position());
        var right = new net.minecraft.world.phys.Vec3(-look.z, 0, look.x);
        float faceCamera = context.computeOnClient(client -> client.player.getYRot() + 180);
        var a = base.add(look.scale(2.6)).add(right.scale(-0.7));
        var b = base.add(look.scale(2.6)).add(right.scale(0.7));
        server.runCommand(String.format(java.util.Locale.ROOT, "summon mannequin %.2f %.2f %.2f {Tags:[\"hlarmor\"],Rotation:[%.1ff,0f],equipment:{head:{id:\"diamond_helmet\",count:1,components:{trim:{material:\"gold\",pattern:\"sentry\"}}},chest:{id:\"diamond_chestplate\",count:1,components:{trim:{material:\"gold\",pattern:\"sentry\"}}},legs:{id:\"diamond_leggings\",count:1},feet:{id:\"diamond_boots\",count:1}}}", a.x, a.y, a.z, faceCamera));
        server.runCommand(String.format(java.util.Locale.ROOT, "summon mannequin %.2f %.2f %.2f {Tags:[\"hlarmor\"],Rotation:[%.1ff,0f],equipment:{head:{id:\"leather_helmet\",count:1,components:{dyed_color:16766720}},chest:{id:\"leather_chestplate\",count:1,components:{dyed_color:16766720}},legs:{id:\"iron_leggings\",count:1},feet:{id:\"netherite_boots\",count:1}}}", b.x, b.y, b.z, faceCamera - 35));
        context.runOnClient(client -> { client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); client.player.setXRot(12); });
        context.waitTicks(25);
        context.takeScreenshot("44-armor-3d-mannequins");
        server.runCommand("kill @e[tag=hlarmor]");
        server.runCommand("item replace entity @a armor.head with diamond_helmet");
        server.runCommand("item replace entity @a armor.chest with diamond_chestplate");
        server.runCommand("item replace entity @a armor.legs with diamond_leggings");
        server.runCommand("item replace entity @a armor.feet with diamond_boots");
        context.runOnClient(client -> client.player.setXRot(70));
        context.waitTicks(15);
        context.takeScreenshot("44-armor-3d-first-person");
        context.runOnClient(client -> { client.player.setXRot(0); client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); });
        context.waitTicks(15);
        context.takeScreenshot("44-armor-3d-front");
        boolean loaded = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("armor_3d");
        context.runOnClient(client -> { client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); client.player.setXRot(0); });
        server.runCommand("clear @a");
        check(loaded, "3D Armor is loaded");
    }

    /** A boombox carried in the off hand stays in the hand when you right-click a block; only the main hand places it. */
    private static void offhandBoomboxStays(ClientGameTestContext context, TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("clear @a");
        server.runCommand("item replace entity @a weapon.offhand with holylois:boombox");
        BlockPos base = context.computeOnClient(client -> client.player.blockPosition());
        BlockPos ground = base.offset(2, -1, 0);
        context.getInput().lookAt(ground);
        context.waitTicks(5);
        context.getInput().pressKey(options -> options.keyUse);
        context.waitTicks(10);
        boolean placed = server.computeOnServer(s -> s.overworld().getBlockState(ground.above()).is(holylois.boombox.Boombox.BLOCK));
        boolean kept = context.computeOnClient(client -> client.player.getOffhandItem().is(holylois.boombox.Boombox.ITEM));
        check(!placed && kept, "an off-hand boombox is not set down by a right-click on a block (placed " + placed + ", still held " + kept + ")");
        server.runCommand("clear @a");
    }

    /** Punching dungeon loot out of an invisible item frame drops the item but never the frame; a normal frame still drops itself. */
    /** Calls a static method of Ji AFK Cinematic by name (tests drive the mod directly instead of waiting for its timers). */
    private static Object ji(String type, String method, Object... args) {
        try {
            for (var m : Class.forName("com.ji.afkcinematic." + type).getDeclaredMethods())
                if (m.getName().equals(method) && m.getParameterCount() == args.length) { m.setAccessible(true); return m.invoke(null, args); }
            throw new NoSuchMethodException(method);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    /**
     * Fishing cinematic (owner 2026-10-10): while waiting for a bite every shot is a new angle on you and the bobber, never one shot on
     * a loop; after reeling in that framing stays 3 s. With boombox music nearby neither Ji's soundtrack nor Minecraft's music plays.
     */
    private static void fishingCinematicAngles(ClientGameTestContext context, TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("clear @a");
        server.runCommand("fill 150 -61 151 160 -61 164 water");
        server.runCommand("tp @a 155 -60 150 0 30");
        server.runCommand("item replace entity @a weapon.mainhand with fishing_rod");
        world.getConnection().waitForChunksRender();
        context.waitTicks(10);
        context.runOnClient(client -> client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND));
        context.waitFor(client -> client.player.fishing != null, 200);
        context.waitTicks(40);
        log("bobber: " + context.computeOnClient(client -> client.player.fishing == null ? "gone" : client.player.fishing.blockPosition().toShortString()
            + " water " + client.player.fishing.isInWater() + " held " + client.player.getMainHandItem()));
        context.runOnClient(client -> ji("cinematic.CinematicManager", "startCinematic", true));
        context.waitTicks(30);
        var angles = new java.util.ArrayList<String>();
        var spots = new java.util.ArrayList<net.minecraft.world.phys.Vec3>();
        for (int shot = 0; shot < 4; shot++) {
            angles.add(context.computeOnClient(client -> holylois.boombox.CinematicTweaks.fishingAngle()));
            spots.add(context.computeOnClient(client -> client.gameRenderer.mainCamera().position()));
            context.runOnClient(client -> ji("cinematic.CinematicManager", "advanceShot"));
            context.waitTicks(20);
        }
        boolean waiting = context.computeOnClient(client -> holylois.boombox.CinematicTweaks.fishingWait);
        double toHook = context.computeOnClient(client -> client.gameRenderer.mainCamera().position().distanceTo(client.player.fishing.position()));
        double toPlayer = context.computeOnClient(client -> client.gameRenderer.mainCamera().position().distanceTo(client.player.position()));
        double moved = spots.get(0).distanceTo(spots.get(1)) + spots.get(1).distanceTo(spots.get(2));
        log("fishing cinematic: waiting " + waiting + ", angles " + angles + ", camera moved " + moved + ", to hook " + toHook + ", to player " + toPlayer);
        check(waiting, "waiting for a bite uses the fishing framing");
        check(new java.util.HashSet<>(angles).size() >= 3, "every shot while waiting is a new angle (" + angles + ")");
        check(moved > 1 && toHook < 16 && toPlayer < 16, "the camera frames you and the bobber from changing spots (" + moved + ", " + toHook + ", " + toPlayer + ")");
        // Reel in: the framing stays 3 s, then the normal shots continue.
        context.runOnClient(client -> client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND));
        context.waitTicks(20);
        boolean held = context.computeOnClient(client -> holylois.boombox.CinematicTweaks.fishingWait);
        context.waitTicks(60);
        boolean released = context.computeOnClient(client -> !holylois.boombox.CinematicTweaks.fishingWait);
        log("after reeling in: framing held at 1 s " + held + ", released at 4 s " + released);
        check(held && released, "after reeling in the fishing framing stays about 3 s (" + held + ", " + released + ")");
        context.runOnClient(client -> ji("cinematic.CinematicManager", "forceDeactivate"));
        // The fishing cinematic above had no boombox, so its soundtrack plays; let it fade out first.
        context.waitFor(client -> !(boolean) field(classOf("com.ji.afkcinematic.music.CinematicMusicManager"), "isOurMusicPlaying"), 400);
        // Soundtrack: Ji's own music must not start while boombox music reaches you; Minecraft's music stays off too.
        java.util.UUID me = context.computeOnClient(client -> client.player.getUUID());
        context.runOnClient(client -> holylois.boombox.BoomboxPulse.heardHeld(me, music(0)));
        log("music diagnostics: nearby " + context.computeOnClient(client -> holylois.boombox.BoomboxPulse.musicNearby()) + ", hooks woven "
            + java.util.Arrays.stream(classOf("com.ji.afkcinematic.music.CinematicMusicManager").getDeclaredMethods()).filter(m -> m.getName().contains("holyLois")).count());
        context.runOnClient(client -> ji("cinematic.CinematicManager", "toggleImmediate"));
        boolean ours = false, vanilla = false;
        for (int t = 0; t < 60; t++) {
            int frame = t;
            context.runOnClient(client -> holylois.boombox.BoomboxPulse.heardHeld(me, music(frame)));
            context.waitTicks(1);
            ours |= context.computeOnClient(client -> (boolean) field(classOf("com.ji.afkcinematic.music.CinematicMusicManager"), "isOurMusicPlaying"));
            vanilla |= context.computeOnClient(client -> field(client.getMusicManager(), "currentMusic") != null);
        }
        context.runOnClient(client -> ji("cinematic.CinematicManager", "forceDeactivate"));
        log("cinematic with boombox music: Ji soundtrack " + ours + ", Minecraft music " + vanilla);
        check(!ours && !vanilla, "with boombox music the cinematic adds no other music (" + ours + ", " + vanilla + ")");
        server.runCommand("clear @a");
    }

    private static Class<?> classOf(String name) {
        try { return Class.forName(name); } catch (ClassNotFoundException error) { throw new IllegalStateException(error); }
    }

    /** Boombox music sits in your ears only while the cinematic camera runs, fading in and out over two seconds (owner 2026-10-10). */
    private static void musicInEarsDuringCinematic(ClientGameTestContext context, TestSingleplayerContext world) {
        var far = new net.minecraft.world.phys.Vec3(0, 0, 0);
        java.util.function.Function<net.minecraft.client.Minecraft, Double> gap = client -> {
            var at = holylois.boombox.BoomboxInEar.blend(far.add(client.player.position()).add(12, 0, 0), "boombox");
            return at.distanceTo(client.gameRenderer.mainCamera().position());
        };
        double before = context.computeOnClient(gap::apply);
        context.runOnClient(client -> { try { Class.forName("com.ji.afkcinematic.cinematic.CinematicManager").getMethod("toggleImmediate").invoke(null); } catch (ReflectiveOperationException e) { throw new RuntimeException(e); } });
        context.waitTicks(50);
        boolean playing = context.computeOnClient(client -> holylois.boombox.CinematicTweaks.playing());
        double during = context.computeOnClient(gap::apply);
        String voice = context.computeOnClient(client -> String.valueOf(holylois.boombox.BoomboxInEar.blend(client.player.position().add(12, 0, 0), "voice")
            .distanceTo(client.player.position().add(12, 0, 0))));
        context.runOnClient(client -> { try { Class.forName("com.ji.afkcinematic.cinematic.CinematicManager").getMethod("forceDeactivate").invoke(null); } catch (ReflectiveOperationException e) { throw new RuntimeException(e); } });
        context.waitTicks(50);
        double after = context.computeOnClient(gap::apply);
        log("in-ear: before " + before + ", cinematic " + playing + " during " + during + ", after " + after + ", voice moved " + voice);
        check(playing, "the cinematic camera started");
        check(before > 8 && during < 0.5 && after > 8, "music is in your ears only while the cinematic runs (" + before + ", " + during + ", " + after + ")");
        check(Double.parseDouble(voice) < 1e-9, "voices stay where they are");
    }

    /** Death loot picked up again lands in its old slots (hotbar, main, armor, off-hand); a slot taken since gets the normal pickup. */
    private static void deathLootBackToSlots(ClientGameTestContext context, TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("kill @e[type=item]");
        server.runCommand("clear @a");
        server.runCommand("tp @a 120 -60 120");
        world.getConnection().waitForChunksRender();
        server.runOnServer(s -> {
            var inventory = s.getPlayerList().getPlayers().getFirst().getInventory();
            inventory.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TORCH, 5));
            inventory.setItem(4, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
            inventory.setItem(20, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 10));
            inventory.setItem(39, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
            inventory.setItem(40, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHIELD));
        });
        server.runCommand("kill @a");
        context.waitForScreen(DeathScreen.class);
        context.waitTicks(20);
        int remembered = server.computeOnServer(s -> holylois.boombox.DeathSlots.remembered());
        context.clickScreenButton("deathScreen.respawn");
        var screens = new java.util.LinkedHashSet<String>();
        boolean faded = false;
        for (int t = 0; t < 60 && !faded; t++) {
            screens.add(context.computeOnClient(client -> client.gui.screen() == null ? "none" : client.gui.screen().getClass().getSimpleName()));
            faded = context.computeOnClient(client -> holylois.auth.PanoramaFade.fading());
            context.waitTicks(1);
        }
        log("respawn screens: " + screens + ", faded " + faded);
        context.waitFor(client -> client.player != null && client.player.isAlive(), 600);
        check(!faded, "respawning after death shows no panorama fade (owner 2026-10-10)");
        // Something new in the old torch slot: the torches must then take the normal pickup.
        server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT)));
        server.runCommand("tp @a 120 -60 120");
        world.getConnection().waitForChunksRender();
        // The stacks keep their death-throw speed and scatter, so the player touches each one directly (the same playerTouch a walk-over runs).
        context.waitTicks(50);
        server.runOnServer(s -> {
            var player = s.getPlayerList().getPlayers().getFirst();
            for (var item : s.overworld().getEntities((net.minecraft.world.entity.Entity) null, player.getBoundingBox().inflate(32), e -> e instanceof net.minecraft.world.entity.item.ItemEntity))
                item.playerTouch(player);
        });
        context.waitTicks(5);
        log("death loot items left: " + server.computeOnServer(s -> String.join(",", s.overworld().getEntities((net.minecraft.world.entity.Entity) null,
            s.getPlayerList().getPlayers().getFirst().getBoundingBox().inflate(64), e -> e instanceof net.minecraft.world.entity.item.ItemEntity).stream()
            .map(e -> ((net.minecraft.world.entity.item.ItemEntity) e).getItem() + "@" + e.blockPosition().toShortString() + " delay=" + ((net.minecraft.world.entity.item.ItemEntity) e).hasPickUpDelay()).toList()))
            + " player@" + server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().blockPosition().toShortString() + " " + s.getPlayerList().getPlayers().getFirst().gameMode.getGameModeForPlayer()));
        String slots = server.computeOnServer(s -> {
            var inventory = s.getPlayerList().getPlayers().getFirst().getInventory();
            var parts = new java.util.ArrayList<String>();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                var stack = inventory.getItem(i);
                if (!stack.isEmpty()) parts.add(i + "=" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + "x" + stack.getCount());
            }
            return String.join(",", parts);
        });
        log("death loot: remembered " + remembered + ", after pickup " + slots);
        check(remembered >= 5, "every dropped stack remembered its slot (" + remembered + ")");
        var list = java.util.Arrays.asList(slots.split(","));
        check(list.contains("4=diamond_swordx1") && list.contains("20=breadx10") && list.contains("39=iron_helmetx1") && list.contains("40=shieldx1"),
            "sword, bread, helmet and shield are back in their slots (" + slots + ")");
        check(list.contains("0=dirtx1") && slots.contains("torchx5") && !list.contains("0=torchx5"), "the torches took the normal pickup around the dirt (" + slots + ")");
        server.runCommand("clear @a");
    }

    private static void invisibleFrameDrops(ClientGameTestContext context, TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("kill @e[type=item]");
        server.runCommand("fill 40 -61 40 44 -59 40 stone");
        server.runCommand("summon item_frame 41 -60 41 {Facing:3b,Invisible:1b,Tags:[\"holylois_frame_test\"],Item:{id:\"minecraft:diamond\",count:1}}");
        server.runCommand("summon item_frame 43 -60 41 {Facing:3b,Tags:[\"holylois_frame_test\"],Item:{id:\"minecraft:emerald\",count:1}}");
        context.waitTicks(5);
        for (int hit = 0; hit < 2; hit++) {
            server.runOnServer(s -> {
                var player = s.getPlayerList().getPlayers().getFirst();
                for (var frame : s.overworld().getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(39, -62, 39, 45, -57, 43), e -> e instanceof net.minecraft.world.entity.decoration.ItemFrame && e.entityTags().contains("holylois_frame_test")))
                    frame.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(player), 1);
            });
            context.waitTicks(5);
        }
        String drops = server.computeOnServer(s -> String.join(",", s.overworld().getEntities((net.minecraft.world.entity.Entity) null,
            new net.minecraft.world.phys.AABB(39, -62, 39, 45, -57, 43), e -> e instanceof net.minecraft.world.entity.item.ItemEntity).stream()
            .map(e -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(((net.minecraft.world.entity.item.ItemEntity) e).getItem().getItem()).getPath()).sorted().toList()));
        log("frame drops: " + drops);
        check(drops.contains("diamond") && drops.contains("emerald"), "both framed items drop when punched out (" + drops + ")");
        check(drops.split("item_frame", -1).length - 1 == 1, "only the visible frame drops itself, the invisible one never does (" + drops + ")");
        server.runCommand("kill @e[type=item]");
    }

    /** 20 ms of a fake track at 48 kHz: a loud low kick every fourth frame over a quiet bed. */
    private static short[] music(int frame) {
        short[] audio = new short[960];
        for (int i = 0; i < audio.length; i++)
            audio[i] = (short) ((frame % 4 == 0 ? 20000 : 600) * Math.sin(2 * Math.PI * 55 * i / 48000.0));
        return audio;
    }

    /** Held boombox playing, seen from the front (F5): notes hop out from the carrying hand on the beat, sized by loudness. */
    private static void boomboxHeldNotes(ClientGameTestContext context, TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("clear @a");
        server.runCommand("item replace entity @a weapon.mainhand with holylois:boombox");
        context.runOnClient(client -> { client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); client.player.setXRot(10); });
        context.waitTicks(10);
        java.util.UUID me = context.computeOnClient(client -> client.player.getUUID());
        for (int t = 0; t <= 80; t++) {
            int frame = t;
            context.runOnClient(client -> holylois.boombox.BoomboxPulse.heardHeld(me, music(frame)));
            context.waitTicks(1);
        }
        context.takeScreenshot("17-boombox-held-notes");
        int notes = context.computeOnClient(client -> (int) field(holylois.boombox.BoomboxPulse.class, "beats"));
        log("held boombox notes: " + notes);
        check(notes > 3, "a held boombox pops notes on the beat (" + notes + ")");
        // Notes leave from the speakers (owner 2026-10-10): the cones sit low at the hand, not up at the hip.
        String speakers = context.computeOnClient(client -> {
            var at = holylois.boombox.BoomboxPulse.heldSpeakers.get(client.player.getUUID());
            return at == null ? "none" : String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", at.left().y - client.player.getY(), at.right().y - client.player.getY(),
                at.left().add(at.right()).scale(0.5).subtract(client.player.position()).horizontalDistance());
        });
        log("held speakers (height left, right, distance): " + speakers);
        check(!speakers.equals("none"), "the held boombox reports its speaker positions");
        if (!speakers.equals("none")) {
            var parts = speakers.split(" ");
            check(Double.parseDouble(parts[0]) < 0.7 && Double.parseDouble(parts[0]) > -0.1 && Double.parseDouble(parts[2]) < 1.2,
                "the speakers hang low at the hand (" + speakers + ")");
        }
        // Swing direction: forced both ways, the speakers must move out and in from the body, not forward and back.
        java.util.function.Function<Float, net.minecraft.world.phys.Vec3> swungTo = side -> {
            context.runOnClient(client -> holylois.boombox.HeldSwing.testSide = side);
            for (int t = 0; t < 6; t++) { int frame = t; context.runOnClient(client -> holylois.boombox.BoomboxPulse.heardHeld(me, music(frame))); context.waitTicks(1); }
            return context.computeOnClient(client -> { var at = holylois.boombox.BoomboxPulse.heldSpeakers.get(client.player.getUUID()); return at.left().add(at.right()).scale(0.5); });
        };
        var out = swungTo.apply(25f); var in = swungTo.apply(-25f);
        context.runOnClient(client -> holylois.boombox.HeldSwing.testSide = null);
        double yaw = Math.toRadians(context.computeOnClient(client -> client.player.yBodyRot));
        var delta = out.subtract(in);
        double sideways = Math.abs(delta.x * -Math.cos(yaw) + delta.z * -Math.sin(yaw)), forward = Math.abs(delta.x * -Math.sin(yaw) + delta.z * Math.cos(yaw));
        log(String.format(java.util.Locale.ROOT, "boombox swing: sideways %.3f, forward %.3f, up %.3f", sideways, forward, delta.y));
        check(sideways > 0.05 && sideways > 3 * forward, "the boombox swings out from the body and back in, not forward (" + sideways + " vs " + forward + ")");
        float size = context.computeOnClient(client -> holylois.boombox.BoomboxPulse.lastScale);
        log("held boombox note size: " + size);
        check(size > 0.3f && size <= 1.45f/2f, "held notes use the owner-approved 1.5x size (" + size + ")");
        // First person (owner 2026-10-10): the boombox in your own hand pulses, but throws no notes into your view.
        context.runOnClient(client -> { client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); client.player.setXRot(65); });
        int conesBefore = context.computeOnClient(client -> holylois.boombox.BoomboxPulse.ownCones);
        int notesBefore = context.computeOnClient(client -> (int) field(holylois.boombox.BoomboxPulse.class, "beats"));
        for (int t = 0; t <= 60; t++) {
            int frame = t;
            context.runOnClient(client -> holylois.boombox.BoomboxPulse.heardHeld(me, music(frame)));
            context.waitTicks(1);
        }
        context.takeScreenshot("17-boombox-held-first-person-playing");
        int cones = context.computeOnClient(client -> holylois.boombox.BoomboxPulse.ownCones) - conesBefore;
        int firstNotes = context.computeOnClient(client -> (int) field(holylois.boombox.BoomboxPulse.class, "beats")) - notesBefore;
        log("first-person boombox: " + cones + " cone frames, " + firstNotes + " notes");
        check(cones > 10 && firstNotes == 0, "the boombox in your own hand pulses without notes in first person (" + cones + ", " + firstNotes + ")");
        server.runCommand("item replace entity @a weapon.mainhand with fishing_rod");
        server.runCommand("item replace entity @a weapon.offhand with holylois:boombox");
        context.waitTicks(5);
        // Fishing camera changes must never reuse a camera-space/GUI position as a world particle anchor.
        for (var camera : net.minecraft.client.CameraType.values()) {
            context.runOnClient(client -> { client.options.setCameraType(camera); client.player.setXRot(-45); });
            for (int t=0;t<18;t++) { int frame=t; context.runOnClient(client -> holylois.boombox.BoomboxPulse.heardHeld(me,music(frame))); context.waitTicks(1); }
            if (camera.isFirstPerson()) continue;
            context.runOnClient(client -> {
                var position=holylois.boombox.BoomboxPulse.lastNotePosition;
                check(position != null && position.distanceTo(client.player.position()) < 3, "Offhand notes stay at the player after camera switch: " + position);
            });
        }
        context.runOnClient(client -> client.gui.setScreen(new InventoryScreen(client.player)));
        context.waitTicks(5);
        context.runOnClient(client -> {
            var at=holylois.boombox.BoomboxPulse.heldSpeakers.get(me);
            check(at == null || at.left().distanceTo(client.player.position()) < 3, "Inventory avatar cannot seed sky notes");
            client.gui.setScreen(null);
        });
        context.takeScreenshot("17-boombox-offhand-after-camera-switch");
        server.runCommand("clear @a");
    }

    /** The client half of structure titles: the server says "you are in a pillager outpost", the title shows below Jade. */
    private static void structureTitle(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(s -> {
            var player = s.getPlayerList().getPlayers().getFirst();
            check(net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, holylois.boombox.StructureZone.TYPE), "client accepts structure zones");
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new holylois.boombox.StructureZone("minecraft:pillager_outpost", 77));
        });
        context.waitTicks(25);
        context.takeScreenshot("14-structure-title");
    }

    /** /capture panorama 1: six square faces in screenshots/holylois-panorama-*. */
    private static void panoramaCapture(ClientGameTestContext context, TestSingleplayerContext world) throws Exception {
        int fovBefore = context.computeOnClient(client -> client.options.fov().get());
        context.runOnClient(client -> client.player.connection.sendCommand("capture panorama 1"));
        context.waitTicks(6 * 20 + 40);
        var root = context.computeOnClient(client -> client.gameDirectory.toPath().resolve("screenshots"));
        java.nio.file.Path folder;
        try (var list = java.nio.file.Files.list(root)) {
            folder = list.filter(path -> path.getFileName().toString().startsWith("holylois-panorama-")).max(java.util.Comparator.naturalOrder()).orElse(null);
        }
        check(folder != null, "panorama folder made");
        for (int face = 0; face < 6; face++) {
            var image = javax.imageio.ImageIO.read(folder.resolve("panorama_" + face + ".png").toFile());
            check(image != null && image.getWidth() == image.getHeight() && image.getWidth() > 100, "panorama face " + face + " is a square image");
        }
        boolean hudBack = context.computeOnClient(client -> !client.gui.hud.isHidden() && client.options.fov().get() == fovBefore);
        check(hudBack, "HUD and field of view restored after the capture");
    }

    private static int deathMarkers() {
        var session = BuiltInHudModules.MINIMAP.getCurrentSession();
        var root = session == null ? null : session.getWorldManager().getCurrentRootContainer();
        if (root == null) return -1;
        int count = 0;
        for (MinimapWorld minimapWorld : root.getAllWorldsIterable())
            for (var set : minimapWorld.getIterableWaypointSets())
                for (Waypoint point : set.getWaypoints())
                    if (point.getPurpose() == WaypointPurpose.DEATH || point.getPurpose() == WaypointPurpose.OLD_DEATH) count++;
        return count;
    }

    private static void log(String text) { org.slf4j.LoggerFactory.getLogger("HolyLoisTest").info(text); }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Holy Lois client test failed: " + what);
        org.slf4j.LoggerFactory.getLogger("HolyLoisTest").info("PASS {}", what);
    }
}
