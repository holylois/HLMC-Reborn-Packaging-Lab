package holylois.boombox;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.UUID;

/** Only Loom development servers: two predictable record opponents, owned by the requesting operator. */
public final class JournalFixture {
    private static final Map<UUID,UUID> targets=new java.util.HashMap<>();
    private JournalFixture() {}
    static void register(){
        if(!FabricLoader.getInstance().isDevelopmentEnvironment())return;
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(Commands.literal("previewjournal")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("stronger").executes(c->spawn(c.getSource().getPlayerOrException(),100)))
            .then(Commands.literal("weaker").executes(c->spawn(c.getSource().getPlayerOrException(),10)))
            .then(Commands.literal("kill").executes(c->{var player=c.getSource().getPlayerOrException();var level=(ServerLevel)player.level();var id=targets.remove(player.getUUID());
                if(id==null||!(level.getEntity(id) instanceof Zombie zombie)){player.sendSystemMessage(Component.literal("Spawn your private Journal opponent first: /previewjournal stronger"));return 0;}
                zombie.setLastHurtByPlayer(player,100);zombie.hurtServer(level,level.damageSources().playerAttack(player),1_000_000);return 1;}))));
    }
    private static int spawn(ServerPlayer player,int value) {
        if(!PartySupport.ready(player))return 0;var level=(ServerLevel)player.level();
        var old=targets.remove(player.getUUID());if(old!=null&&level.getEntity(old) instanceof Zombie zombie)zombie.discard();
        if(targets.size()>=64){player.sendSystemMessage(Component.literal("Private Journal fixture limit reached."));return 0;}
        var zombie=(Zombie)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:zombie")).create(level,EntitySpawnReason.COMMAND);
        zombie.setNoAi(true);zombie.setSilent(true);zombie.setPos(player.getX()+3,player.getY(),player.getZ());
        try{
            var access=Class.forName("com.ultimatearpg.monster.ArpgMonsterLevelAccess");
            access.getMethod("ultimatearpg$setMonsterLevel",int.class).invoke(zombie,value);access.getMethod("ultimatearpg$setMonsterBaseLevel",int.class).invoke(zombie,value);
            for(String method:java.util.List.of("ultimatearpg$setProcessedEliteRoll","ultimatearpg$setProcessedWeaponQuality","ultimatearpg$setProcessedArmorQuality"))access.getMethod(method,boolean.class).invoke(zombie,true);
        }catch(ReflectiveOperationException ignored){}
        zombie.setItemSlot(EquipmentSlot.HEAD,new ItemStack(value>10?Items.DIAMOND_HELMET:Items.LEATHER_HELMET));
        zombie.setItemSlot(EquipmentSlot.CHEST,new ItemStack(value>10?Items.IRON_CHESTPLATE:Items.LEATHER_CHESTPLATE));
        zombie.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
        zombie.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.STRENGTH,1200,1));
        level.addFreshEntity(zombie);targets.put(player.getUUID(),zombie.getUUID());
        player.sendSystemMessage(Component.literal("Private level "+value+" Zombie is ready. Record its defeat with /previewjournal kill, then open /journal."));return 1;
    }
}
