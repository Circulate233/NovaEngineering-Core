package github.kasuminova.novaeng.common.registry;

import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.block.BlockExtendedFacing;
import github.kasuminova.novaeng.common.block.BlockExtendedOre;
import github.kasuminova.novaeng.common.block.BlockExtendedSubstrate;
import github.kasuminova.novaeng.common.entity.EntityExtendedArrow;
import github.kasuminova.novaeng.common.item.ItemBasic;
import github.kasuminova.novaeng.common.item.ItemBlockExtended;
import github.kasuminova.novaeng.common.item.ItemExtendedArrow;
import github.kasuminova.novaeng.common.item.ItemExtendedFood;
import github.kasuminova.novaeng.common.item.ItemExtendedMultiTool;
import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;
import java.util.UUID;

/**
 * 原 Additions 模组 `novaextended` 与 `pie` 两个扩展包内容的注册入口。
 *
 * <p>这些内容原以数据包形式定义，现改由本模组实现。注册名已规范化为
 * `类型_材料` 形式，但矿物词典名保持原样，以避免改动大量既有脚本。
 */
public final class RegistryExtended {

    private RegistryExtended() {
    }

    public static BlockExtendedOre WILLOWALLOY_ORE;
    public static BlockExtendedOre INFINITY_ORE;
    public static BlockExtendedFacing POTATO_SERVER;
    public static BlockExtendedSubstrate WILLOWALLOY_SUBSTRATE;
    public static BlockExtendedSubstrate SERVER_TIER_1;
    public static ItemExtendedMultiTool MULTI_TOOL;
    public static ItemExtendedArrow ARROW_ITEM;

    public static void registerBlocks() {
        WILLOWALLOY_ORE = RegistryBlocks.registerBlock(
            new BlockExtendedOre("willowalloy_ore", 30.0F, 2000.0F, 5, 3));
        INFINITY_ORE = RegistryBlocks.registerBlock(
            new BlockExtendedOre("infinity_ore", 75.0F, 50000.0F, 7, 3));
        POTATO_SERVER = RegistryBlocks.registerBlock(
            new BlockExtendedFacing("potato_server", 5.0F, 50.0F, 2, 0));
        WILLOWALLOY_SUBSTRATE = RegistryBlocks.registerBlock(
            new BlockExtendedSubstrate("willowalloy_substrate", 3.0F, 20.0F, 2));
        SERVER_TIER_1 = RegistryBlocks.registerBlock(
            new BlockExtendedSubstrate("server_tier_1", 5.0F, 20.0F, 3));
        SERVER_TIER_1.setLightLevel(5.0F / 15.0F);

        RegistryBlocks.prepareItemBlockRegister(WILLOWALLOY_ORE);
        RegistryBlocks.prepareItemBlockRegister(INFINITY_ORE);
        RegistryBlocks.prepareItemBlockRegister(WILLOWALLOY_SUBSTRATE);
        RegistryBlocks.prepareItemBlockRegister(new ItemBlockExtended(POTATO_SERVER,
            UUID.fromString("5e9d1d1b-157a-4493-b6a3-ab440095e8ee"), "Block modifier", 0.05D));
        RegistryBlocks.prepareItemBlockRegister(new ItemBlockExtended(SERVER_TIER_1,
            UUID.fromString("b6bde9af-07c2-40d1-a15f-71cf858447cd"), "Block modifier", -0.05D));
    }

    /**
     * 声明由 {@link ItemBasic} 承载的材料与勋章，必须在物品注册事件之前调用。
     *
     * <p>参数依次为 注册名、堆叠数、附魔光效、能否用于信标。
     */
    public static void declareBasicItems() {
        declare("ingot_ark", 64, true, false);
        declare("ingot_blue_alloy", 64, false, false);
        declare("ingot_terra_alloy", 64, false, false);
        declare("ingot_psi_alloy", 64, false, false);
        declare("ingot_machalloy", 64, false, false);
        declare("ingot_willowalloy", 64, false, false);
        declare("ingot_fallen_star_alloy", 64, false, false);
        declare("circuit_ark", 64, false, false);
        declare("circuit_extreme", 64, false, false);
        declare("gem_crystal_rgp", 64, true, false);
        declare("crystal_life_essence", 64, false, false);
        declare("photon_core_variant", 64, false, false);
        declare("seed_omnipotent", 64, false, false);

        declare("medal_radiant", 64, false, true);
        declare("medal_radiant_auriga", 1, true, true);
        declare("medal_radiant_horologium", 64, true, true);
        declare("medal_radiant_chamaeleon", 64, true, true);
        declare("medal_radiant_bootes", 64, true, false);
        declare("medal_radiant_vivid", 64, true, false);
        declare("medal_chaotic_devour", 1, true, false);
        declare("medal_chaotic_invert", 1, true, false);
    }

    private static void declare(final String name, final int maxStack,
                                final boolean glint, final boolean beaconPayment) {
        ItemBasic.Companion.declare(
            name, maxStack, glint, beaconPayment);
    }

    public static void registerItems() {
        MULTI_TOOL = RegistryItems.registerItem(new ItemExtendedMultiTool());
        ARROW_ITEM = RegistryItems.registerItem(new ItemExtendedArrow());

        EntityRegistry.registerModEntity(
            new ResourceLocation(
                Tags.MOD_ID, "extended_arrow"),
            EntityExtendedArrow.class,
            "extended_arrow", 64, NovaEngineeringCore.instance, 1, 1, true);

        RegistryItems.registerItem(new ItemExtendedFood("pie_apple",
            12, 0.3F, 32, List.of(ItemExtendedFood.speed(30, 2))));
        RegistryItems.registerItem(new ItemExtendedFood("pie_chocolate",
            12, 0.3F, 32, List.of(ItemExtendedFood.speed(600, 2))));
        RegistryItems.registerItem(new ItemExtendedFood("pie_shepherds",
            14, 5.0F / 7.0F, 40, List.of(
            ItemExtendedFood.regeneration(6000, 1),
            ItemExtendedFood.absorption(6000, 4, 0.5F),
            ItemExtendedFood.resistance(6000, 2))));
        RegistryItems.registerItem(new ItemExtendedFood("pie_sweet_berry_cheesecake",
            12, 0.3F, 32, List.of(ItemExtendedFood.strength(1200, 1))));
    }

    /**
     * 注册数据包中声明的矿物词典名，脚本依据这些名称引用材料。
     *
     * <p>必须在物品注册表填充完成后调用，避免把尚未入册的实例写进矿物词典。
     */
    public static void registerOreDict() {
        oreDict("oreWillowalloy", WILLOWALLOY_ORE);
        oreDict("oreInfinitySplinter", INFINITY_ORE);

        oreDict("ingotArk", "ingot_ark");
        oreDict("ingotBlueAlloy", "ingot_blue_alloy");
        oreDict("ingotTerraAlloy", "ingot_terra_alloy");
        oreDict("ingotPsiAlloy", "ingot_psi_alloy");
        oreDict("ingotMachalloy", "ingot_machalloy");
        oreDict("ingotWillowalloy", "ingot_willowalloy");
        oreDict("ingotFallenStarAlloy", "ingot_fallen_star_alloy");
        oreDict("circuitArk", "circuit_ark");
        oreDict("circuitExtreme", "circuit_extreme");
        oreDict("gemCrystalRGP", "gem_crystal_rgp");
        oreDict("crystalLifeessence", "crystal_life_essence");
    }

    private static void oreDict(final String oreName, final Block block) {
        final Item item = Item.getItemFromBlock(block);
        if (item != null) {
            OreDictionary.registerOre(oreName, new ItemStack(item));
        }
    }

    private static void oreDict(final String oreName, final String itemPath) {
        final Item item = ItemBasic.Companion.getItem(itemPath);
        if (item != null) {
            OreDictionary.registerOre(oreName, new ItemStack(item));
        }
    }

}
