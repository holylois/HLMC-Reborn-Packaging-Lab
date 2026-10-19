package holylois.boombox.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import holylois.boombox.RelicLooks;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/** Worn relics look like themselves (RelicLooks.worn): the Circlet is a thin band, not a gold helmet; the Apron is an apron. */
@Mixin(HumanoidArmorLayer.class)
public abstract class RelicWornMixin {
    @WrapOperation(method = "renderArmorPiece", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/equipment/Equippable;assetId()Ljava/util/Optional;"), require = 1)
    private Optional<ResourceKey<EquipmentAsset>> holyLoisRelicWorn(Equippable equippable, Operation<Optional<ResourceKey<EquipmentAsset>>> original,
                                                                    @Local(argsOnly = true) ItemStack stack) {
        var worn = RelicLooks.worn(stack);
        return worn != null ? Optional.of(worn) : original.call(equippable);
    }
}
