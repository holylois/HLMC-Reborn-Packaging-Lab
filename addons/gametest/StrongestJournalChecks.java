package holylois;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import java.util.List;
import holylois.boombox.*;

/** Actual production die hook, confirmation queue and save data, in a test-only account-free backend. */
public final class StrongestJournalChecks {
    private static Discoveries backend;
    private static final java.util.Set<java.util.UUID> cancelled=new java.util.HashSet<>();
    public static void register(){
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{if(backend!=null&&entity.level() instanceof ServerLevel level)backend.killed(level.getServer(),entity);});
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DEATH.register((entity,source,damage)->{if(cancelled.remove(entity.getUUID())){entity.setHealth(5);return false;}return true;});
    }
    public static void run(ClientGameTestContext context,TestSingleplayerContext world)throws Exception{
        world.getServer().runOnServer(server->{
            backend=new Discoveries();backend.load(server,server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("journal-record-fixture"));
            kill(server,20,Items.IRON_HELMET,false);
        });context.waitTicks(4);
        world.getServer().runOnServer(server->{backend.drainDeaths(server);kill(server,50,Items.DIAMOND_HELMET,false);});context.waitTicks(4);
        world.getServer().runOnServer(server->{backend.drainDeaths(server);kill(server,10,Items.LEATHER_HELMET,false);kill(server,99,Items.NETHERITE_HELMET,true);});context.waitTicks(4);
        var page=world.getServer().computeOnServer(server->{
            backend.drainDeaths(server);var player=server.getPlayerList().getPlayers().getFirst();var snapshot=backend.snapshot(player,new JournalRequest(1,3,0,8,"zombie"));
            var record=snapshot.entries().getFirst().strongest();
            check(record!=null&&record.level()==50,"weaker/cancelled deaths do not replace the strongest confirmed kill");
            check(record.gear().stream().anyMatch(g->g.item().equals("minecraft:diamond_helmet"))&&record.gear().stream().anyMatch(g->g.item().equals("minecraft:iron_sword")),"gear is saved before death drops clear equipment");
            check(record.effects().stream().anyMatch(e->e.id().equals("minecraft:strength"))&&record.affixes().contains(com.ultimatearpg.monster.ArpgEliteAffix.HEAVY_ARMOR.id()),"actual effects and semicolon Runeforged affix IDs saved");
            return snapshot;
        });
        var firstPage=world.getServer().computeOnServer(server->{
            var player=server.getPlayerList().getPlayers().getFirst();var first=backend.snapshot(player,new JournalRequest(0,3,0,8,"zombie"));
            var mob=first.entries().stream().filter(e->e.key().equals("mob:minecraft:zombie")).findFirst().orElseThrow();
            check(mob.strongest()!=null&&mob.strongest().level()==20,"world first retains the original defeat, separate from strongest level 50");
            check(mob.strongest().gear().stream().anyMatch(g->g.item().equals("minecraft:iron_helmet")),"world first retains original equipment");
            check(mob.subject().equals("minecraft:zombie"),"world-first subject survives the page payload");
            return first;
        });
        world.getServer().runOnServer(server->kill(server,"minecraft:witch",31,Items.IRON_HELMET,false));context.waitTicks(4);
        var witchPage=world.getServer().computeOnServer(server->{backend.drainDeaths(server);var player=server.getPlayerList().getPlayers().getFirst();return backend.snapshot(player,new JournalRequest(1,3,0,8,"witch"));});
        var witchFirst=world.getServer().computeOnServer(server->{var player=server.getPlayerList().getPlayers().getFirst();return backend.snapshot(player,new JournalRequest(0,3,0,8,"witch"));});
        context.waitTicks(100);
        context.runOnClient(client->{
            var preview=new JournalPreview();var record=page.entries().getFirst().strongest();var entity=preview.entity(record);
            check(entity!=null&&client.level.getEntity(entity.getId())==null,"preview zombie is never inserted into the world");
            check(entity.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET),"isolated preview restores strongest equipment");
            check(entity.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_SWORD),"isolated preview restores the recorded weapon");
            String details=JournalScreen.recordLines(record).stream().map(c->c.getString()).collect(java.util.stream.Collectors.joining("\n"));
            check(details.contains("Maximum health")&&!details.contains("Helmet")&&!details.contains("Sword")&&!details.contains("Strength"),"details keep HP without redundant gear or potion lines");
            check(details.contains("Traits")&&details.contains("Haste")&&details.contains("Physical Resistance"),"nameplate traits are preserved and readable in the defeat record");
            var witch=preview.entity(witchPage.entries().getFirst().strongest());
            check(witch instanceof net.minecraft.world.entity.monster.Witch&&client.level.getEntity(witch.getId())==null,"Witch record creates an isolated Witch preview");
            check(witch.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_SWORD),"Witch equipment snapshot is restored");
            check(witchFirst.entries().getFirst().strongest().type().equals("minecraft:witch"),"Witch world first has its own appearance snapshot");
            check(JournalScreen.detailOverflow(99,100)==0&&JournalScreen.detailOverflow(102,100)==0&&JournalScreen.detailOverflow(150,100)==48,"fitting text has no scrolling; overflow remains reachable");
            client.options.guiScale().set(1);client.resizeGui();
            client.gui.setScreen(new JournalScreen(page));
        });context.waitTicks(1);context.takeScreenshot("60-strongest-zombie-wide");
        var pause=context.computeOnClient(client->{var button=client.gui.screen().children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("Pause")).map(w->(net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();double scale=client.getWindow().getGuiScale();return new double[]{(button.getX()+button.getWidth()/2)*scale,(button.getY()+8)*scale};});
        context.getInput().setCursorPos(pause[0],pause[1]);context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);context.waitTicks(2);
        context.runOnClient(client->{try{var field=JournalScreen.class.getDeclaredField("previewPaused");field.setAccessible(true);check(field.getBoolean(client.gui.screen()),"real mouse click pauses the rotating model");}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}});
        context.runOnClient(client->client.gui.setScreen(new JournalScreen(firstPage)));
        context.waitTicks(4);context.takeScreenshot("60-world-first-zombie-wide");
        context.runOnClient(client->client.gui.setScreen(new JournalScreen(witchPage)));context.waitTicks(4);context.takeScreenshot("60-witch-personal-wide");
        context.runOnClient(client->client.gui.setScreen(new JournalScreen(witchFirst)));context.waitTicks(4);context.takeScreenshot("60-witch-world-first-wide");
        context.runOnClient(client->{client.options.guiScale().set(2);client.resizeGui();client.gui.setScreen(new JournalScreen(new JournalState(1,3,0,1,1,2,"zombie",page.entries())));});
        context.waitTicks(1);context.takeScreenshot("60-strongest-zombie-narrow-list");
        context.runOnClient(client->{
            var screen=new JournalScreen(new JournalState(1,3,0,1,1,2,"zombie",page.entries()));
            try{var detail=JournalScreen.class.getDeclaredField("detail");detail.setAccessible(true);detail.setBoolean(screen,true);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            client.gui.setScreen(screen);
        });context.waitTicks(1);context.takeScreenshot("60-strongest-zombie-narrow-detail");
        context.runOnClient(client->client.gui.setScreen(null));
        world.getServer().runOnServer(server->{backend=null;});
    }
    private static void kill(net.minecraft.server.MinecraftServer server,int level,net.minecraft.world.item.Item helmet,boolean cancel){
        kill(server,"minecraft:zombie",level,helmet,cancel);
    }
    private static void kill(net.minecraft.server.MinecraftServer server,String type,int level,net.minecraft.world.item.Item helmet,boolean cancel){
        var player=server.getPlayerList().getPlayers().getFirst();var world=(ServerLevel)player.level();
        var zombie=(net.minecraft.world.entity.Mob)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(type)).create(world,EntitySpawnReason.COMMAND);
        zombie.setNoAi(true);zombie.setPos(player.getX()+3,player.getY(),player.getZ());
        var rune=(com.ultimatearpg.monster.ArpgMonsterLevelAccess)zombie;rune.ultimatearpg$setMonsterLevel(level);rune.ultimatearpg$setMonsterBaseLevel(level);rune.ultimatearpg$setEliteMonster(true);rune.ultimatearpg$setEliteAffixIds(com.ultimatearpg.monster.ArpgEliteAffix.HEAVY_ARMOR.id()+";"+com.ultimatearpg.monster.ArpgEliteAffix.HASTE.id()+";"+com.ultimatearpg.monster.ArpgEliteAffix.HEAVY.id());rune.ultimatearpg$setProcessedEliteRoll(true);rune.ultimatearpg$setProcessedWeaponQuality(true);rune.ultimatearpg$setProcessedArmorQuality(true);
        zombie.setItemSlot(EquipmentSlot.HEAD,new ItemStack(helmet));zombie.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
        zombie.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.STRENGTH,300,1));
        if(cancel)cancelled.add(zombie.getUUID());world.addFreshEntity(zombie);zombie.setLastHurtByPlayer(player,100);zombie.hurtServer(world,world.damageSources().playerAttack(player),100000);
        if(cancel){check(zombie.isAlive(),"cancelled death remains alive");zombie.discard();}
    }
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);org.slf4j.LoggerFactory.getLogger("HolyLoisTest").info("PASS {}",label);}
}
