package github.kasuminova.novaeng.common.handler;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.item.ItemRawOre;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import lombok.val;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import org.jetbrains.annotations.NotNull;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static crafttweaker.CraftTweakerAPI.itemUtils;

@ZenRegister
@ZenClass("novaeng.hypernet.RawOre")
public class OreHandler {

    public static final OreHandler INSTANCE = new OreHandler();
    public static final Map<String, String> VeinMap = new Object2ObjectOpenHashMap<>();
    public static final Map<String, IItemStack> VeinItemMap = new Object2ObjectOpenHashMap<>();
    private static Map<OreKey, ItemStack> rawOreMap;
    private static Map<OreKey, ItemStack> oreMap;

    private OreHandler() {
    }

    public static IItemStack getRawOre(@NotNull ItemStack ore) {
        return getRawOre(CraftTweakerMC.getIItemStack(ore));
    }

    @ZenMethod
    public static IItemStack getRawOre(@NotNull IItemStack ore) {
        if (rawOreMap.containsKey(OreKey.getKey(CraftTweakerMC.getItemStack(ore)))) {
            return CraftTweakerMC.getIItemStack(rawOreMap.get(OreKey.getKey(CraftTweakerMC.getItemStack(ore))));
        } else {
            return null;
        }
    }

    public static IItemStack getOre(@NotNull ItemStack ore) {
        return getOre(CraftTweakerMC.getIItemStack(ore));
    }

    @ZenMethod
    public static IItemStack getOre(@NotNull IItemStack ore) {
        if (oreMap.containsKey(OreKey.getKey(CraftTweakerMC.getItemStack(ore)))) {
            return CraftTweakerMC.getIItemStack(oreMap.get(OreKey.getKey(CraftTweakerMC.getItemStack(ore))));
        } else {
            return ore;
        }
    }

    @ZenMethod
    public static void regOreVein(String name, String oreVeinItemName) {
        VeinMap.put(name, oreVeinItemName);
    }

    @ZenMethod
    public static IItemStack getOreVeinItem(String name) {
        var out = VeinItemMap.get(name);
        if (out == null) {
            out = itemUtils.getItem("contenttweaker:" + VeinMap.get(name), 0);
            VeinMap.remove(name);
            VeinItemMap.put(name, out);
        }
        return out;
    }

    public static void registry() {
        Object2ReferenceMap<OreKey, ItemStack> map = new Object2ReferenceOpenHashMap<>();
        Object2ReferenceMap<OreKey, ItemStack> mapO = new Object2ReferenceOpenHashMap<>();

        for (var entry : ItemRawOre.getRawOreAndName()) {
            val rawOreName = entry.getKey();
            val rawOreItem = entry.getValue();
            ItemStack rawOre = new ItemStack(rawOreItem);
            final String oreName = rawOreItem.getOreOD();
            if (!OreDictionary.getOres(oreName).isEmpty()) {
                var ores = OreDictionary.getOres(oreName);
                final ItemStack ODore = OreDictHelper.getPriorityItemFromOreDict(oreName, ores);
                for (ItemStack ore : ores) {
                    var ok = OreKey.getKey(ore);
                    map.put(ok, rawOre);
                    mapO.put(ok, ODore);
                    NovaEngineeringCore.log.info("registered : {}[{}]", ok.toString(), rawOreName);
                }
            }
        }

        rawOreMap = Object2ReferenceMaps.unmodifiable(map);
        oreMap = Object2ReferenceMaps.unmodifiable(mapO);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onHarvestDropsEvent(BlockEvent.HarvestDropsEvent event) {
        if (!event.getWorld().isRemote) {
            IBlockState blockState = event.getState();
            Block block = blockState.getBlock();
            int meta = block.getMetaFromState(blockState);
            final OreKey key = OreKey.getKey(block, meta);
            if (rawOreMap.containsKey(key)) {
                List<ItemStack> drops = event.getDrops();
                drops.clear();
                if (event.isSilkTouching()) {
                    ItemStack ore = oreMap.get(key).copy();
                    ore.setCount(1);
                    if (drops.isEmpty()) {
                        drops.add(ore);
                    }
                    return;
                }

                int fortune = event.getFortuneLevel();
                int random = event.getWorld().rand.nextInt((fortune + 2));
                ItemStack rawOre = rawOreMap.get(key).copy();
                rawOre.setCount(Math.max(random, 1));
                if (drops.isEmpty()) {
                    drops.add(rawOre);
                }
            }
        }
    }

    private final static class OreKey {
        private static final OreKey redStone = new OreKey(new ItemStack(Blocks.REDSTONE_ORE));
        private final ItemStack item;
        private String toString;
        private int hash = -1;

        private OreKey(ItemStack item) {
            this.item = item;
        }

        public static OreKey getKey(ItemStack itemStack) {
            return new OreKey(itemStack);
        }

        public static OreKey getKey(Block block, int meta) {
            if (block == Blocks.LIT_REDSTONE_ORE) return redStone;
            return new OreKey(new ItemStack(block, 1, meta));
        }

        public int getMetadata() {
            return item.getMetadata();
        }

        public ResourceLocation getRl() {
            return item.getItem().getRegistryName();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof OreKey oreKey)) return false;
            return this.getMetadata() == oreKey.getMetadata() && Objects.equals(this.getRl(), oreKey.getRl());
        }

        @Override
        public int hashCode() {
            if (hash == -1) {
                hash = Objects.hash(this.getRl(), this.getMetadata());
            }
            return hash;
        }

        @Override
        public String toString() {
            if (toString == null) {
                toString = this.getRl().toString() + ":" + this.getMetadata();
            }
            return toString;
        }

    }

    public static class OreDictHelper {
        private static final String[] MOD_PRIORITY = {
                "minecraft",
                "thermalfoundation",
                "ic2",
                "mekanism",
                "immersiveengineering"
        };

        public static ItemStack getPriorityItemFromOreDict(String oreName) {
            return getPriorityItemFromOreDict(oreName, OreDictionary.getOres(oreName));
        }

        public static ItemStack getPriorityItemFromOreDict(String oreName, List<ItemStack> oreEntries) {
            return switch (oreEntries.size()) {
                case 0 -> ItemStack.EMPTY;
                case 1 -> oreEntries.get(0).copy();
                default -> {
                    if ("dimensional_shard_ore".equals(oreName)) {
                        ItemStack item = oreEntries.get(0).copy();
                        item.setItemDamage(0);
                        yield item;
                    }

                    ItemStack candidates = ItemStack.EMPTY;

                    for (String modid : MOD_PRIORITY) {
                        for (ItemStack stack : oreEntries) {
                            String itemModID = stack.getItem().getRegistryName().getNamespace();
                            if (modid.equals(itemModID)) {
                                candidates = stack.copy();
                                break;
                            }
                        }
                        if (!candidates.isEmpty()) break;
                    }

                    var out = candidates.isEmpty() ? oreEntries.get(0).copy() : candidates;
                    if (out.getItemDamage() == 32767) out.setItemDamage(0);
                    yield out;
                }
            };
        }
    }
}
