package github.kasuminova.novaeng.common.handler;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import github.kasuminova.novaeng.NovaEngineeringCore;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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

@ZenRegister
@ZenClass("novaeng.hypernet.RawOre")
public class OreHandler {

    public static final OreHandler INSTANCE = new OreHandler();

    private static final String rawOreOD = "rawOre";
    private static final String rawOreGemOD = "rawOreGem";
    private static final String oreOD = "ore";
    private static Map<OreKey, ItemStack> rawOreMap;
    private static Map<OreKey, ItemStack> oreMap;

    private OreHandler(){}

    @ZenMethod
    public static IItemStack getRawOre(@NotNull IItemStack ore){
        if (rawOreMap.containsKey(OreKey.getKey(CraftTweakerMC.getItemStack(ore)))){
            return CraftTweakerMC.getIItemStack(rawOreMap.get(OreKey.getKey(CraftTweakerMC.getItemStack(ore))));
        } else {
            return null;
        }
    }

    @ZenMethod
    public static IItemStack getOre(@NotNull IItemStack ore){
        if (oreMap.containsKey(OreKey.getKey(CraftTweakerMC.getItemStack(ore)))){
            return CraftTweakerMC.getIItemStack(oreMap.get(OreKey.getKey(CraftTweakerMC.getItemStack(ore))));
        } else {
            return ore;
        }
    }

    public static void registry() {
        Object2ObjectMap<OreKey, ItemStack> map = new Object2ObjectOpenHashMap<>();
        Object2ObjectMap<OreKey, ItemStack> mapO = new Object2ObjectOpenHashMap<>();

        for (String odName : OreDictionary.getOreNames()) {
            if (odName.startsWith(rawOreOD)) {
                if (!OreDictionary.getOres(odName).isEmpty()) {
                    ItemStack rawOre = OreDictionary.getOres(odName).get(0);
                    String rawOreName = odName.startsWith(rawOreGemOD) ?
                            odName.substring(rawOreGemOD.length()) :
                            odName.substring(rawOreOD.length());

                    final String oreName = oreOD + rawOreName;
                    if (!OreDictionary.getOres(oreName).isEmpty()) {
                        var ores = OreDictionary.getOres(oreName);
                        final ItemStack ODore = OreDictHelper.getPriorityItemFromOreDict(oreName);
                        for (ItemStack ore : ores) {
                            var ok = OreKey.getKey(ore);
                            map.put(ok, rawOre);
                            mapO.put(ok, ODore);
                            NovaEngineeringCore.log.info("registered : {}[{}]", ok.toString(), rawOreName);
                        }
                    }
                }
            }
        }

        rawOreMap = Object2ObjectMaps.unmodifiable(map);
        oreMap = Object2ObjectMaps.unmodifiable(mapO);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onHarvestDropsEvent(BlockEvent.HarvestDropsEvent event) {
        if (!event.getWorld().isRemote) {
            IBlockState blockState = event.getState();
            Block block = blockState.getBlock();
            int meta = block.getMetaFromState(blockState);
            final OreKey key = OreKey.getKey(block,meta);
            if (rawOreMap.containsKey(key)){
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
                if (drops.isEmpty()){
                    drops.add(rawOre);
                }
            }
        }
    }

    private final static class OreKey {
        private final ItemStack item;
        private String toString;
        private int hash = -1;

        private OreKey(ItemStack item){
            this.item = item;
        }

        public int getMetadata(){
            return item.getMetadata();
        }

        public ResourceLocation getRl(){
            return item.getItem().getRegistryName();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof OreKey oreKey)) return false;
            return this.getMetadata() == oreKey.getMetadata() && Objects.equals(this.getRl(),oreKey.getRl());
        }

        @Override
        public int hashCode() {
            if (hash == -1){
                hash = Objects.hash(this.getRl(), this.getMetadata());
            }
            return hash;
        }

        @Override
        public String toString(){
            if (toString == null){
                toString = this.getRl().toString() + ":" + this.getMetadata();
            }
            return toString;
        }

        public static OreKey getKey(ItemStack itemStack) {
            return new OreKey(itemStack);
        }

        private static final OreKey redStone = new OreKey(new ItemStack(Blocks.REDSTONE_ORE));

        public static OreKey getKey(Block block,int meta) {
            if (block == Blocks.LIT_REDSTONE_ORE)return redStone;
            return new OreKey(new ItemStack(block,1,meta));
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
            List<ItemStack> oreEntries = OreDictionary.getOres(oreName);

            return switch (oreEntries.size()){
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
                        if (!candidates.isEmpty())break;
                    }

                    var out = candidates.isEmpty() ? oreEntries.get(0).copy() : candidates;
                    if (out.getItemDamage() == 32767)out.setItemDamage(0);
                    yield out;
                }
            };
        }
    }
}
