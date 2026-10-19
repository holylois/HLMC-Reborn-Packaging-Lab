package holylois;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import java.util.List;

/** Smelting (autosmelt:auto_smelt, built into Holy Lois Extras since 2.2.0) on real ore loot and the vein-mining path. */
public final class SmeltingChecks {
    public static void run(ClientGameTestContext context,TestSingleplayerContext world)throws Exception{
        world.getServer().runOnServer(server->{
            var player=server.getPlayerList().getPlayers().getFirst();player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var level=server.overworld();var registry=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var smelt=registry.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,Identifier.parse("autosmelt:auto_smelt")));
            var fortune=registry.getOrThrow(Enchantments.FORTUNE);var silk=registry.getOrThrow(Enchantments.SILK_TOUCH);
            check(smelt.is(TagKey.create(Registries.ENCHANTMENT,Identifier.parse("minecraft:in_enchanting_table"))),"Smelting is in enchanting-table pool");
            check(smelt.is(TagKey.create(Registries.ENCHANTMENT,Identifier.parse("minecraft:tradeable"))),"Smelting books available to villagers");
            check(!Enchantment.areCompatible(smelt,silk)&&Enchantment.areCompatible(smelt,fortune),"Silk Touch exclusive; Fortune compatible");
            for(var item:List.of(Items.DIAMOND_PICKAXE,Items.BOOK))check(EnchantmentHelper.getAvailableEnchantmentResults(30,new ItemStack(item),java.util.stream.Stream.of(smelt)).stream().anyMatch(e->e.enchantment().equals(smelt)),"Table can roll Smelting on "+item);
            var pos=player.blockPosition().offset(8,0,0);var plain=new ItemStack(Items.DIAMOND_PICKAXE);plain.enchant(fortune,3);
            var tool=plain.copy();tool.enchant(smelt,1);
            var ores=List.of(Blocks.IRON_ORE,Blocks.COPPER_ORE,Blocks.GOLD_ORE,Blocks.ANCIENT_DEBRIS);
            var outputs=List.of(Items.IRON_INGOT,Items.COPPER_INGOT,Items.GOLD_INGOT,Items.NETHERITE_SCRAP);
            for(int i=0;i<ores.size();i++){
                var output=outputs.get(i);
                var raw=drops(level,player,pos,ores.get(i),plain);
                var cooked=drops(level,player,pos,ores.get(i),tool);
                check(!cooked.isEmpty()&&cooked.stream().allMatch(s->s.is(output))&&count(raw)==count(cooked),"Ore converts without changing Fortune quantity: "+ores.get(i));
            }
            var forced=tool.copy();forced.enchant(silk,1);var drops=Block.getDrops(Blocks.IRON_ORE.defaultBlockState(),level,pos,null,player,forced);
            check(drops.size()==1&&drops.getFirst().is(Items.IRON_ORE),"Forced Silk Touch still keeps ore block");
            player.getInventory().setSelectedSlot(8);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tool);player.getFoodData().setFoodLevel(20);
            com.iamkaf.liteminer.Liteminer.instance.onKeymappingStateChange(player,true,0);
            for(int i=0;i<3;i++)level.setBlock(pos.offset(i,0,0),Blocks.IRON_ORE.defaultBlockState(),3);
            check(player.gameMode.destroyBlock(pos),"Actual survival ore break succeeds");
        });
        context.waitTicks(10);
        world.getServer().runOnServer(server->{var player=server.getPlayerList().getPlayers().getFirst();var pos=player.blockPosition().offset(8,0,0);var level=server.overworld();
            check(level.getBlockState(pos).isAir()&&level.getBlockState(pos.offset(1,0,0)).isAir()&&level.getBlockState(pos.offset(2,0,0)).isAir(),"Liteminer breaks connected ore vein");
            var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(4));
            check(drops.stream().anyMatch(e->e.getItem().is(Items.IRON_INGOT))&&drops.stream().noneMatch(e->e.getItem().is(Items.RAW_IRON)),"Vein-mined ore drops ingots, no raw duplicates");
            com.iamkaf.liteminer.Liteminer.instance.onKeymappingStateChange(player,false,0);
        });
        context.takeScreenshot("63-smelting-compatible");
    }
    private static int count(List<ItemStack> drops){return drops.stream().mapToInt(ItemStack::getCount).sum();}
    private static List<ItemStack> drops(net.minecraft.server.level.ServerLevel level,net.minecraft.server.level.ServerPlayer player,BlockPos pos,Block block,ItemStack tool){
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE,block.defaultBlockState())
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.atCenterOf(pos))
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL,tool)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,player)
            .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
        return level.getServer().reloadableRegistries().getLootTable(block.getLootTable().orElseThrow()).getRandomItems(params,123L);
    }
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);org.slf4j.LoggerFactory.getLogger("HolyLoisTest").info("PASS {}",label);}
}
