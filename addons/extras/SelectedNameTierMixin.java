package holylois.boombox.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import holylois.boombox.HolyRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** The item name shown over the hotbar when you switch items: our tier colour and shine instead of vanilla's rarity colour, not italic. */
@Mixin(Hud.class)
public abstract class SelectedNameTierMixin {
    @Shadow private ItemStack lastToolHighlight;

    @WrapOperation(method = "extractSelectedItemName", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;"), require = 2)
    private MutableComponent holyLoisTierName(MutableComponent name, ChatFormatting format, Operation<MutableComponent> original) {
        String tier = HolyRarity.of(lastToolHighlight);
        if (tier.isEmpty()) return original.call(name, format);
        return HolyRarity.title(lastToolHighlight, name, tier).copy();
    }
}
