package github.kasuminova.novaeng.common.item;

import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.registry.RegistryBlocks;
import github.kasuminova.novaeng.common.util.StringUtils;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ReferenceCollection;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.oredict.OreDictionary;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static github.kasuminova.novaeng.common.registry.RegistryItems.ITEMS_TO_REGISTER;

public final class ItemRawOre extends Item {

    private static final Object2ReferenceMap<String, ItemRawOre> allItemRawOre = new Object2ReferenceOpenHashMap<>();
    private static final CreativeTabs rawOreTab = new CreativeTabs("raw_ore") {
        private static ItemStack Icon;

        @Override
        @NotNull
        public ItemStack createIcon() {
            if (Icon == null) {
                for (var value : allItemRawOre.values()) {
                    return Icon = new ItemStack(value);
                }
            }
            return Icon;
        }
    };
    private static final Object2ReferenceMap<String, BlockRawOre> allItemRawBlock = new Object2ReferenceOpenHashMap<>();
    private static final Object2ReferenceMap<String, BlockRawOre.ItemBLockRawOre> allItemRawBlockItem = new Object2ReferenceOpenHashMap<>();

    @Getter
    private final String rawOD;
    @Getter
    private final String oreOD;
    @Getter
    private final String partOD;
    @Getter
    private final String name;
    @Getter
    private final Type type;

    public ItemRawOre(String name, Type type) {
        if (type == Type.BLOCK) throw new RuntimeException("Directly registering BlockRawOre is not allowed");
        this.name = StringUtils.camelToSnake(name);
        this.type = type;
        this.setCreativeTab(rawOreTab);
        var key = type.getName() + "_" + this.name;
        this.setRegistryName(NovaEngineeringCore.getRL(key));
        this.setTranslationKey(NovaEngineeringCore.MOD_ID + '.' + key);
        rawOD = type.getOdName(name);
        oreOD = "ore" + name;
        partOD = type.name().toLowerCase() + name;
        allItemRawOre.put(this.name, this);
        allItemRawBlock.put(this.name, new BlockRawOre(name));
        OreDictionary.registerOre(this.getRawOD(), this);
    }

    public static ItemRawOre getRawOre(String name) {
        return allItemRawOre.get(name);
    }

    public static BlockRawOre getRawBlock(String name) {
        return allItemRawBlock.get(name);
    }

    public static BlockRawOre.ItemBLockRawOre getRawItemBlock(String name) {
        return allItemRawBlockItem.get(name);
    }

    public static ReferenceCollection<ItemRawOre> getRawOres() {
        return allItemRawOre.values();
    }

    public static ObjectSet<Map.Entry<String, ItemRawOre>> getRawOreAndName() {
        return allItemRawOre.entrySet();
    }

    public static ReferenceCollection<BlockRawOre> getRawBlocks() {
        return allItemRawBlock.values();
    }

    public static void rawOreRegister(String name) {
        rawOreRegister(name, Type.INGOT);
    }

    public static void rawOreRegister(String name, int color) {
        var item = new ItemRawOre(name, Type.INGOT);
        ITEMS_TO_REGISTER.add(item);
        NovaEngineeringCore.proxy.setColor(item.partOD, color);
    }

    public static void rawOreRegister(String name, Type type) {
        ITEMS_TO_REGISTER.add(new ItemRawOre(name, type));
    }

    public static void rawOreRegister(String name, Type type, int color) {
        var item = new ItemRawOre(name, type);
        ITEMS_TO_REGISTER.add(item);
        NovaEngineeringCore.proxy.setColor(item.partOD, color);
    }

    public static void rawOreGemRegister(String name) {
        ITEMS_TO_REGISTER.add(new ItemRawOre(name, Type.GEM));
    }

    public static void rawOreGemRegister(String name, int color) {
        var item = new ItemRawOre(name, Type.GEM);
        ITEMS_TO_REGISTER.add(item);
        NovaEngineeringCore.proxy.setColor(item.partOD, color);
    }

    public static void regAll() {
        rawOreRegister("Gold", 0xffff00);
        rawOreRegister("Copper");
        rawOreRegister("Osmium");
        rawOreRegister("Iron", 0xfedec8);
        rawOreRegister("AncientDebris");
        rawOreRegister("Ardite");
        rawOreRegister("Cobalt");
        rawOreRegister("Aluminium");
        rawOreRegister("AstralStarmetal", 0x232e68);
        rawOreRegister("Boron");
        rawOreRegister("Dilithium");
        rawOreRegister("Iridium");
        rawOreRegister("Lithium");
        rawOreRegister("Platinum");
        rawOreRegister("Nickel");
        rawOreRegister("Mithril");
        rawOreRegister("Magnesium");
        rawOreRegister("Thorium");
        rawOreRegister("Titanium");
        rawOreRegister("Uranium");
        rawOreRegister("Willowalloy");
        rawOreRegister("Tin");
        rawOreRegister("Silver");
        rawOreRegister("Lead");
        rawOreRegister("Uru");
        rawOreRegister("Osram");
        rawOreRegister("Eezo");
        rawOreRegister("Abyssum");
        rawOreRegister("Aurorium");
        rawOreRegister("Valyrium");
        rawOreRegister("Vibranium");
        rawOreRegister("Karmesine");
        rawOreRegister("Ovium");
        rawOreRegister("Jauxum");
        rawOreRegister("Duranite");
        rawOreRegister("Prometheum");
        rawOreRegister("Palladium");
        rawOreRegister("Tiberium");
        rawOreRegister("Redstone", Type.DUST, 0xff0000);

        rawOreGemRegister("Diamond");
        rawOreGemRegister("Lapis");
        rawOreGemRegister("Emerald");
        rawOreGemRegister("Coal");
        rawOreGemRegister("Quartz");
        rawOreGemRegister("Aquamarine");
        rawOreGemRegister("Amethyst");
        rawOreGemRegister("Ruby");
        rawOreGemRegister("Amber");
        rawOreGemRegister("Peridot");
        rawOreGemRegister("Sapphire");
        rawOreGemRegister("Malachite");
        rawOreGemRegister("Tanzanite");
        rawOreGemRegister("Topaz");
        rawOreGemRegister("Fluorite");
        rawOreGemRegister("CertusQuartz");
        rawOreGemRegister("ChargedCertusQuartz");
        rawOreGemRegister("DimensionalShard");
        rawOreGemRegister("QuartzBlack", 0x312c2c);

        for (var rawBlock : getRawBlocks()) {
            RegistryBlocks.prepareItemBlockRegister(RegistryBlocks.registerBlock(rawBlock).getItem());
        }
    }

    @Override
    @NotNull
    public String getItemStackDisplayName(@NotNull ItemStack stack) {
        if (I18n.canTranslate(this.getTranslationKey(stack) + ".name")) return super.getItemStackDisplayName(stack);
        return I18n.translateToLocalFormatted(type.localizationKey, I18n.translateToLocal("base.material." + name));
    }

    public enum Type {
        INGOT("rawOre"),
        GEM("rawOreGem"),
        BLOCK("rawBlock"),
        DUST("rawOre");

        @Getter
        @NotNull
        private final String odName;
        @Getter
        @NotNull
        private final String name;
        @Getter
        @NotNull
        private final String localizationKey;
        @Getter
        @NotNull
        private final String defR;

        Type(@NotNull String name) {
            this.odName = name;
            this.name = StringUtils.camelToSnake(name);
            this.localizationKey = "novaeng.part." + this.name;
            this.defR = this.name().equals("BLOCK") ?
                "blocks/raw_block/raw_block" : "items/raw_ore/" + this.name;
        }

        public String getOdName(String name) {
            return odName + name;
        }
    }

    @Getter
    public class BlockRawOre extends Block {

        @Getter
        private final String rawOD;
        @Getter
        private final Type type = Type.BLOCK;
        @Getter
        private final ItemBLockRawOre item;

        private BlockRawOre(String name) {
            super(Material.ROCK);
            this.setResistance(10.0F);
            this.setSoundType(SoundType.STONE);
            this.setCreativeTab(rawOreTab);
            this.setDefaultState(this.blockState.getBaseState());
            this.setTranslationKey(NovaEngineeringCore.MOD_ID + '.' + "raw_block_" + ItemRawOre.this.name);
            this.setRegistryName(NovaEngineeringCore.getRL("raw_block_" + ItemRawOre.this.name));
            this.rawOD = type.getOdName(name);
            this.item = new ItemBLockRawOre();
            allItemRawBlockItem.put(ItemRawOre.this.name, this.item);
        }

        public String getPartOD() {
            return ItemRawOre.this.partOD;
        }

        public class ItemBLockRawOre extends ItemBlock {

            private ItemBLockRawOre() {
                super(BlockRawOre.this);
                this.setCreativeTab(rawOreTab);
                this.setTranslationKey(BlockRawOre.this.getTranslationKey());
                this.setRegistryName(BlockRawOre.this.getRegistryName());
                OreDictionary.registerOre(BlockRawOre.this.getRawOD(), this);
            }

            public String getPartOD() {
                return ItemRawOre.this.partOD;
            }

            @Override
            @NotNull
            public String getItemStackDisplayName(@NotNull ItemStack stack) {
                if (I18n.canTranslate(this.getTranslationKey(stack) + ".name"))
                    return super.getItemStackDisplayName(stack);
                return I18n.translateToLocalFormatted(type.localizationKey, I18n.translateToLocal("base.material." + name));
            }
        }

    }
}