package holylois.boombox.mixins;

import holylois.boombox.HolyRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Item names and chat item links use the Holy Lois tier colour instead of the vanilla rarity colour (HolyRarity). Low priority: callbacks at RETURN run in reverse priority order, so this one runs after Runeforged restyles the name. */
@Mixin(value = ItemStack.class, priority = 500)
public abstract class HolyRarityNameMixin {
    @Inject(method = "getStyledHoverName", at = @At("RETURN"), cancellable = true, require = 1)
    private void holyLoisTierName(CallbackInfoReturnable<Component> info) {
        String tier = HolyRarity.of((ItemStack) (Object) this);
        if (!tier.isEmpty()) info.setReturnValue(HolyRarity.style(info.getReturnValue(), tier));
    }

    /**
     * Runeforged rebuilds the tooltip after the name is styled: its title and "Quality: X" line come back in its own colours.
     * Repaint both in the tier colour, and name the quality in our words (Cursed kinds keep Runeforged's name, in Cursed violet).
     */
    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true, require = 1)
    private void holyLoisTierTooltip(net.minecraft.world.item.Item.TooltipContext context, net.minecraft.world.entity.player.Player player,
                                     net.minecraft.world.item.TooltipFlag flag, CallbackInfoReturnable<java.util.List<Component>> info) {
        String tier = HolyRarity.of((ItemStack) (Object) this);
        var lines = info.getReturnValue();
        if (lines == null || lines.isEmpty()) return;
        if (tier.isEmpty()) { var plain = new java.util.ArrayList<Component>(); for (var line : lines) plain.add(HolyRarity.recolor(line)); write(info, lines, plain); return; }
        int color = holylois.boombox.FishData.color(tier) & 0xFFFFFF;
        var out = new java.util.ArrayList<Component>();
        for (var line : lines) out.add(HolyRarity.recolor(line));
        boolean italic = Boolean.TRUE.equals(out.getFirst().getStyle().isItalic());
        var self = (ItemStack) (Object) this;
        // Fish and crates style their own names (Shiny star, crate tip); everything else gets one clean tier-coloured title.
        if (holylois.boombox.FishData.rarity(self).isEmpty() && holylois.boombox.CrateStyle.rarity(self).isEmpty())
            out.set(0, Component.literal(out.getFirst().getString()).withStyle(s -> holylois.boombox.FishData.shine(s.withColor(color).withItalic(italic), tier)));
        for (int i = 1; i < out.size(); i++) {
            if (!(out.get(i).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents quality)
                || !quality.getKey().equals("tooltip.runeforged.quality_line")) continue;
            Component word = tier.equals("cursed") && quality.getArgs().length > 0 && quality.getArgs()[0] instanceof Component own
                ? Component.literal(own.getString()) : Component.translatable("holylois.journal.rarity" + HolyRarity.rank(tier));
            out.set(i, Component.translatable("tooltip.runeforged.quality_line", word.copy().withStyle(s -> s.withColor(color)))
                .withStyle(out.get(i).getStyle()));
        }
        write(info, lines, out);
    }

    /** Edits the list in place: Fabric's tooltip callback (crate lines) may add to this same list after us. */
    private static void write(CallbackInfoReturnable<java.util.List<Component>> info, java.util.List<Component> lines, java.util.List<Component> out) {
        try { for (int i = 0; i < out.size(); i++) lines.set(i, out.get(i)); }
        catch (UnsupportedOperationException immutable) { info.setReturnValue(out); }
    }

    /** Trophy fish already glow in their rarity colour: the enchant glint stays only on Mythic (colossal included) and Shiny. */
    @Inject(method = "hasFoil", at = @At("RETURN"), cancellable = true, require = 1)
    private void holyLoisFishGlint(CallbackInfoReturnable<Boolean> info) {
        var stack = (ItemStack) (Object) this;
        if (info.getReturnValueZ() && holylois.boombox.FishData.weighed(stack) && !stack.isEnchanted()
            && !holylois.boombox.FishData.rarity(stack).equals("mythic") && !holylois.boombox.FishData.shiny(stack)) info.setReturnValue(false);
    }

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true, require = 1)
    private void holyLoisTierLink(CallbackInfoReturnable<Component> info) {
        String tier = HolyRarity.of((ItemStack) (Object) this);
        if (!tier.isEmpty()) info.setReturnValue(HolyRarity.style(info.getReturnValue(), tier));
    }
}
