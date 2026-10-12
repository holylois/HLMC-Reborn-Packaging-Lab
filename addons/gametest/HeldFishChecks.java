package holylois.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import java.util.List;

/** Real item migration/attribute checks and placed note rendering without an internet radio connection. */
final class HeldFishChecks {
    static void run(ClientGameTestContext context,TestSingleplayerContext world){
        world.getServer().runOnServer(server->{
            var player=server.getPlayerList().getPlayers().getFirst();int slot=20;
            for(String species:List.of("minecraft:cod","minecraft:salmon","minecraft:tropical_fish","minecraft:pufferfish","fishofthieves:islehopper")){
                var raw=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(species)));
                var data=new CompoundTag();var tag=new CompoundTag();
                tag.putString("rarity","legendary");tag.putString("species",species);tag.putDouble("kg",25);tag.putBoolean("held_tooltip",true);
                data.put("holylois_fish",tag);raw.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                raw.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("Caught by TestAngler, 2026-10-12"))));
                var eaten=raw.get(DataComponents.CONSUMABLE);holylois.boombox.LowTierFish.normalize(raw);
                var modifier=raw.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().getLast();
                check(modifier.modifier().amount()>0&&java.util.Objects.equals(eaten,raw.get(DataComponents.CONSUMABLE)),species+": migrating held bonus preserves eaten effects");
                var cooked=raw.copy();var cookedData=data.copy();var f=tag.copy();f.putBoolean("cooked",true);f.remove("held_version");cookedData.put("holylois_fish",f);
                cooked.set(DataComponents.CUSTOM_DATA,CustomData.of(cookedData));holylois.boombox.LowTierFish.normalize(cooked);
                check(cooked.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().getLast().modifier().amount()==modifier.modifier().amount()/2,species+": cooked held bonus is half raw");
                check(cooked.get(DataComponents.LORE).lines().stream().filter(l->l.getString().equals("When held:")).count()==1,"held section does not duplicate");
                if(slot<24){player.getInventory().setItem(slot++,raw);player.getInventory().setItem(slot++,cooked);}
            }
            player.inventoryMenu.broadcastChanges();
        });
        world.getServer().runCommand("setblock 4 -61 1 stone");world.getServer().runCommand("setblock 4 -60 1 holylois:boombox[facing=west,playing=true]");
        context.runOnClient(client->{client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);client.player.setYRot(-90);client.player.setXRot(10);holylois.boombox.BoomboxPulse.lastScale=0;});context.waitTicks(5);
        for(int i=0;i<80;i++){int frame=i;context.runOnClient(client->holylois.boombox.BoomboxPulse.heard(4.5,-59.5,1.5,audio(frame)));context.waitTicks(1);}
        float scale=context.computeOnClient(client->holylois.boombox.BoomboxPulse.lastScale);
        check(scale>=0.275f&&scale<=0.725f,"placed notes have half the previous loudness-based size: "+scale);
        context.takeScreenshot("59-half-placed-notes");
        context.runOnClient(client->client.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player)));context.waitTicks(5);context.takeScreenshot("59-vanilla-held-bonuses");
        context.runOnClient(client->client.gui.setScreen(null));world.getServer().runCommand("setblock 4 -60 1 air");
    }
    private static short[] audio(int frame){var samples=new short[960];double volume=frame%12<2?12000:1500;for(int i=0;i<samples.length;i++)samples[i]=(short)(Math.sin(i*0.15)*volume);return samples;}
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);org.slf4j.LoggerFactory.getLogger("HolyLoisTest").info("PASS {}",label);}
}
