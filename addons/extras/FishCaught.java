package holylois.boombox;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Native fishing and hooked schools report catches here. Gifts, commands and pickups do not. */
public final class FishCaught {
    private FishCaught() {}
    public interface Listener { void caught(ServerPlayer player, ItemStack fish); }
    public static final Event<Listener> EVENT=EventFactory.createArrayBacked(Listener.class,listeners -> (player,fish) -> {
        for(var listener:listeners)listener.caught(player,fish);
    });
    static void record(ServerPlayer player,ItemStack stack) {
        var config=Legends.fish;
        if(player==null||stack.isEmpty()||config==null||!PartySupport.ready(player)||FishData.fillet(stack))return;
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean fish=config.species.containsKey(id);
        if(!fish && stack.has(net.minecraft.core.component.DataComponents.FOOD) && config.skip.stream().noneMatch(id::contains)){
            fish=config.namespaces.contains(id.substring(0,id.indexOf(':')));
            for(String tag:config.tags)fish|=stack.is(TagKey.create(Registries.ITEM,Identifier.parse(tag)));
        }
        if(fish)EVENT.invoker().caught(player,stack.copy());
    }
}
