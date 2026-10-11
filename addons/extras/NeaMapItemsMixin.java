package holylois.boombox.mixins;

import dev.tr7zw.notenoughanimations.animations.hands.MapHoldingAnimation;
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
 * Camp maps looked wrong in hand (owner 2026-10-11): 26.3 has explorer map items of their own (abandoned_camp_map from Dungeons
 * and Taverns, buried_treasure_map...), and Not Enough Animations only holds the ones in its config, filled_map by default.
 * Every *_map item joins its list after it is read, so existing players need no config change.
 */
@Pseudo
@Mixin(value = MapHoldingAnimation.class, remap = false)
public abstract class NeaMapItemsMixin {
    @Shadow private Set<Item> compatibleMaps;

    @Inject(method = "bind", at = @At("TAIL"), require = 0)
    private void holyLoisAllMapItems(CallbackInfo info) {
        for (var id : BuiltInRegistries.ITEM.keySet())
            if (id.getNamespace().equals("minecraft") && id.getPath().endsWith("_map")) compatibleMaps.add(BuiltInRegistries.ITEM.getValue(id));
    }
}
