package holylois.boombox.mixins;

import dev.tr7zw.firstperson.LogicHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Set;

/**
 * First person: a camp map showed as a flat icon instead of the open map (owner 2026-10-11). First Person Model only switches to
 * the game's own map hands for filled_map and its config list; every 26.3 *_map item (abandoned_camp_map...) joins that list each
 * time it is rebuilt, so existing players need no config change.
 */
@Pseudo
@Mixin(value = LogicHandler.class, remap = false)
public abstract class FirstPersonMapItemsMixin {
    @Shadow private Set<Item> autoVanillaHandItems;

    @Inject(method = "reloadAutoVanillaHandsSettings", at = @At("TAIL"), require = 0)
    private void holyLoisAllMapItems(CallbackInfo info) {
        for (var id : BuiltInRegistries.ITEM.keySet())
            if (id.getNamespace().equals("minecraft") && id.getPath().endsWith("_map")) autoVanillaHandItems.add(BuiltInRegistries.ITEM.getValue(id));
    }
}
