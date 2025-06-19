package github.kasuminova.novaeng.common.handler;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import github.kasuminova.novaeng.NovaEngineeringCore;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ZenRegister
@ZenClass("novaeng.hypernet.RawOre")
public class RawOreHandler {

    public static final RawOreHandler INSTANCE = new RawOreHandler();

    private static final String rawOreOD = "rawOre";
    private static final String rawOreGemOD = "rawOreGem";
    private static Map<OreKey, ItemStack> rawOreMap;
    private static Map<OreKey, ItemStack> oreMap;
    private RawOreHandler(){}

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

    public static void registry(){
        Map<OreKey,ItemStack> map = new HashMap<>();
        Map<OreKey,ItemStack> mapO = new HashMap<>();

        for (String odName : OreDictionary.getOreNames()) {
            if (odName.startsWith(rawOreOD)){
                if (!OreDictionary.getOres(odName).isEmpty()){
                    ItemStack rawOre = OreDictionary.getOres(odName).get(0);
                    String rawOreName = odName.startsWith(rawOreGemOD) ?
                            odName.substring(rawOreGemOD.length()) :
                            odName.substring(rawOreOD.length());

                    final String oreName = "ore" + rawOreName;
                    if (!OreDictionary.getOres(oreName).isEmpty()) {
                        var ores = OreDictionary.getOres(oreName);
                        final ItemStack ODore = OreDictHelper.getPriorityItemFromOreDict(oreName);
                        for (ItemStack ore : ores){
                            var ok = OreKey.getKey(ore);
                            map.put(ok,rawOre);
                            mapO.put(ok,ODore);

                            if (ore.getItem() instanceof ItemBlock ik && ik.getBlock() == Blocks.REDSTONE_ORE) {
                                ok = OreKey.getKey(Blocks.LIT_REDSTONE_ORE.getRegistryName(), ore.getItemDamage());
                                map.put(ok, rawOre);
                                mapO.put(ok, ODore);
                            }

                            NovaEngineeringCore.log.info("registered : {}[{}]", ok.toString(), rawOreName);
                        }
                    }
                }
            }
        }

        rawOreMap = Collections.unmodifiableMap(map);
        oreMap = Collections.unmodifiableMap(mapO);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onHarvestDropsEvent(BlockEvent.HarvestDropsEvent event) {
        if (!event.getWorld().isRemote) {
            IBlockState blockState = event.getState();
            Block block = blockState.getBlock();
            int meta = block.getMetaFromState(blockState);
            ResourceLocation registryName = block.getRegistryName();
            final OreKey key = OreKey.getKey(registryName,meta);
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
                } else if (registryName != null && registryName.getNamespace().equals("astralsorcery") && hasSilkTouch(event.getHarvester())) {
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

    public static boolean hasSilkTouch(EntityPlayer player) {
        if (player instanceof FakePlayer)return false;
        ItemStack mainHandStack = player.getHeldItemMainhand();
        return EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, mainHandStack) != 0;
    }

    private final static class OreKey {
        private final ResourceLocation rl;
        private final int meta;
        private String toString;
        private int hash = -1;

        private static final Map<ResourceLocation, Map<Integer, OreKey>> keyPool = new HashMap<>();

        private OreKey(ResourceLocation Rl,int Meta){
            this.rl = Rl;
            this.meta = Meta;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof OreKey oreKey)) return false;
            return meta == oreKey.meta && rl.equals(oreKey.rl);
        }

        @Override
        public int hashCode() {
            if (hash == -1){
                hash = Objects.hash(rl, meta);
            }
            return hash;
        }

        @Override
        public String toString(){
            if (toString == null){
                toString = rl.toString() + ":" + meta;
            }
            return toString;
        }

        public static OreKey getKey(ItemStack itemStack) {
            ResourceLocation rl = itemStack.getItem().getRegistryName();
            int meta = itemStack.getItemDamage();
            return keyPool.computeIfAbsent(rl, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(meta, m -> new OreKey(rl, meta));
        }

        public static OreKey getKey(ResourceLocation rl, int meta) {
            return keyPool.computeIfAbsent(rl, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(meta, m -> new OreKey(rl, meta));
        }
    }

    private static class OreDictHelper {
        private static final String[] MOD_PRIORITY = {
                "minecraft",
                "thermalfoundation",
                "ic2",
                "mekanism",
                "immersiveengineering"
        };

        public static ItemStack getPriorityItemFromOreDict(String oreName) {
            List<ItemStack> oreEntries = OreDictionary.getOres(oreName);

            if ("dimensional_shard_ore".equals(oreName)) {
                ItemStack item = oreEntries.get(0).copy();
                item.setItemDamage(0);
                return item;
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
            }

            var out = candidates.isEmpty() ? oreEntries.get(0) : candidates;
            if (out.getItemDamage() == 32767)out.setItemDamage(0);
            return out;
        }

    }
}
