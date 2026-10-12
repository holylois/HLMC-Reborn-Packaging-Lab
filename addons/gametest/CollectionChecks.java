package holylois;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import holylois.boombox.*;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class CollectionChecks {
    public static void run(ClientGameTestContext context,TestSingleplayerContext world)throws Exception{
        var heavy=new JournalFish("fishofthieves:splashtail","fishofthieves:indigo",20,"epic",false,100).at("minecraft:overworld",11,64,22);
        var rare=new JournalFish("fishofthieves:splashtail","fishofthieves:sunny",3,"mythic",true,200).at("minecraft:the_nether",33,70,44);
        var entry=new JournalState.Entry("fish:splashtail",1,heavy.species(),"","minecraft:overworld",99,64,99,300,3,20,40,0,true,false).withFish(heavy).withRarest(rare);
        context.runOnClient(client->{client.options.guiScale().set(1);client.resizeGui();client.player.getInventory().setSelectedSlot(8);client.gui.setScreen(new JournalScreen(new JournalState(1,1,0,1,1,8,"",List.of(entry))));});
        context.waitTicks(4);context.takeScreenshot("62-fish-heaviest");
        var button=context.computeOnClient(client->{var b=client.gui.screen().children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button btn&&btn.getMessage().getString().equals("Heaviest")).map(w->(net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();double scale=client.getWindow().getGuiScale();return new double[]{(b.getX()+b.getWidth()/2)*scale,(b.getY()+8)*scale};});
        context.getInput().setCursorPos(button[0],button[1]);context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);context.waitTicks(3);context.takeScreenshot("62-fish-rarest");
        context.runOnClient(client->{try{var field=JournalScreen.class.getDeclaredField("fishRarest");field.setAccessible(true);check(field.getBoolean(client.gui.screen()),"Real mouse selects Rarest record");}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}client.gui.setScreen(null);});
        world.getServer().runOnServer(server->{var player=server.getPlayerList().getPlayers().getFirst();for(String name:List.of("mythic","shiny_legendary","shiny_mythic")){var holder=server.getAdvancements().get(Identifier.parse("holylois:fishing/"+name));check(holder!=null,"Loaded catch advancement "+name);for(String criterion:holder.value().criteria().keySet())player.getAdvancements().revoke(holder,criterion);}player.getInventory().add(fish("mythic",true));});
        context.waitTicks(5);
        world.getServer().runOnServer(server->{var player=server.getPlayerList().getPlayers().getFirst();check(!done(player,"mythic")&&!done(player,"shiny_mythic"),"Inventory gift does not award real-catch achievements");reel(player,"legendary",true);check(done(player,"shiny_legendary")&&!done(player,"mythic"),"Real Shiny Legendary catch awards only its milestone");reel(player,"mythic",true);check(done(player,"mythic")&&done(player,"shiny_mythic"),"Real Shiny Mythic catch awards both Mythic milestones");});
        context.waitTicks(3);context.takeScreenshot("62-catch-achievements");
    }
    private static net.minecraft.world.item.ItemStack fish(String rarity,boolean shiny){var stack=new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:cod")));var tag=new net.minecraft.nbt.CompoundTag();var data=new net.minecraft.nbt.CompoundTag();data.putString("rarity",rarity);data.putDouble("kg",10);data.putBoolean("shiny",shiny);tag.put("holylois_fish",data);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));return stack;}
    private static void reel(net.minecraft.server.level.ServerPlayer player,String rarity,boolean shiny){var level=(net.minecraft.server.level.ServerLevel)player.level();var fish=(net.minecraft.world.entity.animal.fish.AbstractFish)net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:cod")).create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);fish.setNoAi(true);fish.setPos(player.getX()+2,player.getY(),player.getZ());((SwimmingFish.Memory)fish).holyLoisFish(fish(rarity,shiny));level.addFreshEntity(fish);check(SchoolCatch.reel(player,fish),"Actual school fishing path reels the trophy item");}
    private static boolean done(net.minecraft.server.level.ServerPlayer player,String name){var holder=player.level().getServer().getAdvancements().get(Identifier.parse("holylois:fishing/"+name));return holder!=null&&player.getAdvancements().getOrStartProgress(holder).isDone();}
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);org.slf4j.LoggerFactory.getLogger("HolyLoisTest").info("PASS {}",label);}
}
