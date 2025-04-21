package github.kasuminova.novaeng.common.handler;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import github.kasuminova.novaeng.NovaEngineeringCore;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ZenRegister
@ZenClass("novaeng.hypernet.RawOre")
public class RawOreHandler {

    public static final RawOreHandler INSTANCE = new RawOreHandler();

    private static final String rawOreOD = "rawOre";
    private static Map<OreKey, ItemStack> rawOreMap;
    private static Map<OreKey, ItemStack> oreMap;
    private RawOreHandler(){}

    @ZenMethod
    public static IItemStack getRawOre(IItemStack ore){
        if (rawOreMap.containsKey(OreKey.getKey(CraftTweakerMC.getItemStack(ore)))){
            return CraftTweakerMC.getIItemStack(rawOreMap.get(OreKey.getKey(CraftTweakerMC.getItemStack(ore))));
        } else {
            return null;
        }
    }

    @ZenMethod
    public static IItemStack getOre(IItemStack ore){
        if (oreMap.containsKey(OreKey.getKey(CraftTweakerMC.getItemStack(ore)))){
            return CraftTweakerMC.getIItemStack(oreMap.get(OreKey.getKey(CraftTweakerMC.getItemStack(ore))));
        } else {
            return null;
        }
    }

    public static void registry(){
        Map<OreKey,ItemStack> map = new HashMap<>();
        Map<OreKey,ItemStack> mapO = new HashMap<>();

        for (String oreName : OreDictionary.getOreNames()) {
            if (oreName.startsWith(rawOreOD)){
                if (!OreDictionary.getOres(oreName).isEmpty()){
                    ItemStack rawOre = OreDictionary.getOres(oreName).get(0);
                    String rawOreName = oreName.substring(rawOreOD.length());
                    if (oreName.startsWith(rawOreOD + "Gem")){
                        rawOreName = oreName.substring((rawOreOD + "Gem").length());
                    }

                    if (!OreDictionary.getOres("ore" + rawOreName).isEmpty()) {
                        ItemStack[] ores = OreDictionary.getOres("ore" + rawOreName).toArray(new ItemStack[0]);
                        final ItemStack ODore = OreDictHelper.getPriorityItemFromOreDict("ore" + rawOreName);
                        for (ItemStack ore : ores){
                            ResourceLocation rg = ore.getItem().getRegistryName();
                            map.put(OreKey.getKey(rg,ore.getItemDamage()),rawOre);
                            mapO.put(OreKey.getKey(rg,ore.getItemDamage()),ODore);
                            if (rg != null && rg.getPath().equals("redstone_ore")) {
                                rg = new ResourceLocation("minecraft", "lit_redstone_ore");
                                map.put(OreKey.getKey(rg, ore.getItemDamage()), rawOre);
                                mapO.put(OreKey.getKey(rg, ore.getItemDamage()), ODore);
                            }

                            NovaEngineeringCore.log.info("registered : {}[{}]", rg + ":" + ore.getItemDamage(), rawOreName);
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
                    if (drops.isEmpty()) {
                        drops.add(oreMap.get(key));
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

    private static class OreKey {
        public ResourceLocation rl;
        public int meta;
        private static final Map<ResourceLocation, Map<Integer, OreKey>> keyPool = new HashMap<>();

        private OreKey(ResourceLocation Rl,int Meta){
            rl = Rl;
            meta = Meta;
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

            List<ItemStack> candidates = new ArrayList<>();

            for (String modid : MOD_PRIORITY) {
                for (ItemStack stack : oreEntries) {
                    String itemModID = stack.getItem().getRegistryName().getNamespace();
                    if (modid.equals(itemModID)) {
                        candidates.add(stack);
                        break;
                    }
                }
            }

            return candidates.isEmpty() ? oreEntries.get(0) : candidates.get(0);
        }

    }
}
