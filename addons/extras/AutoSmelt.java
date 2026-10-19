package holylois.boombox;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.ArrayList;
import java.util.List;

/**
 * The Smelting pickaxe enchantment, formerly the separate "Auto Smelt" mod (MIT, Auto Smelt contributors). Same id
 * (autosmelt:auto_smelt) and data, so pickaxes enchanted before keep working: ores in #autosmelt:smeltable_ores drop their
 * smelted result, Fortune counts are kept, Silk Touch wins, creative and spectator players are left alone.
 */
public final class AutoSmelt {
    private AutoSmelt() {}
    static final ResourceKey<Enchantment> AUTO_SMELT = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("autosmelt", "auto_smelt"));
    static final TagKey<Block> SMELTABLE_ORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("autosmelt", "smeltable_ores"));

    public static void register() { LootTableEvents.MODIFY_DROPS.register(AutoSmelt::smelt); }

    private static void smelt(Holder<LootTable> table, LootContext context, List<ItemStack> drops) {
        BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
        ItemInstance tool = context.getOptional(LootContextParams.TOOL);
        if (drops.isEmpty() || state == null || !state.is(SMELTABLE_ORES) || tool == null || !tool.is(ItemTags.PICKAXES)) return;
        if (!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof Player player) || player.isCreative() || player.isSpectator()) return;
        if (state.getBlock().getLootTable().filter(table::is).isEmpty()) return;
        var level = context.getLevel();
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var smelting = enchantments.get(AUTO_SMELT);
        if (smelting.isEmpty() || EnchantmentHelper.getItemEnchantmentLevel(smelting.get(), tool) < 1) return;
        if (enchantments.get(Enchantments.SILK_TOUCH).filter(silk -> EnchantmentHelper.getItemEnchantmentLevel(silk, tool) > 0).isPresent()) return;
        List<ItemStack> out = new ArrayList<>(drops.size());
        boolean changed = false;
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) { out.add(drop); continue; }
            var input = new SingleRecipeInput(drop.copyWithCount(1));
            var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
            ItemStack result = recipe.isEmpty() ? ItemStack.EMPTY : recipe.get().value().assemble(input);
            if (result.isEmpty()) { out.add(drop); continue; }
            changed = true;
            add(out, result, drop.getCount());
        }
        // Nether gold ore drops nuggets: smelt the ore block itself instead (one ingot), like smelting the mined block.
        if (!changed && state.is(Blocks.NETHER_GOLD_ORE)) {
            var input = new SingleRecipeInput(new ItemStack(state.getBlock()));
            var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
            if (recipe.isPresent()) {
                ItemStack result = recipe.get().value().assemble(input);
                if (!result.isEmpty() && !result.is(Items.GOLD_NUGGET) && out.removeIf(s -> s.is(Items.GOLD_NUGGET))) { add(out, result, 1); changed = true; }
            }
        }
        if (changed) { drops.clear(); drops.addAll(out); }
    }

    private static void add(List<ItemStack> out, ItemStack result, int times) {
        long total = (long) times * result.getCount();
        int max = Math.max(1, result.getMaxStackSize());
        while (total > 0) { int n = (int) Math.min(max, total); out.add(result.copyWithCount(n)); total -= n; }
    }
}
