package github.kasuminova.novaeng.mixin;

import github.kasuminova.novaeng.common.config.NovaEngCoreConfig;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static github.kasuminova.novaeng.NovaEngineeringCore.MOD_ID;
import static github.kasuminova.novaeng.mixin.NovaEngCoreEarlyMixinLoader.LOG;
import static github.kasuminova.novaeng.mixin.NovaEngCoreEarlyMixinLoader.LOG_PREFIX;

@SuppressWarnings({"unused", "SameParameterValue"})
public class NovaEngCoreLateMixinLoader implements ILateMixinLoader {

    private static final Map<String, BooleanSupplier> MIXIN_CONFIGS = new Object2ObjectLinkedOpenHashMap<>();

    static {
        addModdedMixinCFG("mixins.novaeng_core_ae2.json",                  "appliedenergistics2");
        addModdedMixinCFG("mixins.novaeng_core_astralsorcery.json",        "astralsorcery");
        addModdedMixinCFG("mixins.novaeng_core_athenaeum.json",            "athenaeum");
        addModdedMixinCFG("mixins.novaeng_core_cofhcore.json",             "cofhcore");
        addModdedMixinCFG("mixins.novaeng_core_draconicevolution.json",    "draconicevolution");
        addModdedMixinCFG("mixins.novaeng_core_ic2.json",                  "ic2");
        addModdedMixinCFG("mixins.novaeng_core_immersiveengineering.json", "immersiveengineering");
        addModdedMixinCFG("mixins.novaeng_core_mets.json",                 "mets");
        addModdedMixinCFG("mixins.novaeng_core_nae2.json",                 "nae2");
        addModdedMixinCFG("mixins.novaeng_core_botania.json",              "botania");
        addModdedMixinCFG("mixins.novaeng_core_jetif.json",                "jetif");
        addModdedMixinCFG("mixins.novaeng_core_opticheck.json",            "opticheck");
        addModdedMixinCFG("mixins.novaeng_core_electroblobs.json",         "ebwizardry");
        addModdedMixinCFG("mixins.novaeng_core_psi.json",                  "psi");
        addModdedMixinCFG("mixins.novaeng_core_libvulpes.json",                  "libvulpes");
        addModdedMixinCFG("mixins.novaeng_core_techguns.json",                  "techguns");
        addModdedMixinCFG("mixins.novaeng_core_codechickenlib.json","codechickenlib");
        addModdedMixinCFG("mixins.novaeng_core_legendarytooltips.json","legendarytooltips");
        addModdedMixinCFG("mixins.novaeng_core_avaritia.json","avaritia");
        addModdedMixinCFG("mixins.novaeng_core_betterp2p.json","betterp2p");
        addModdedMixinCFG("mixins.novaeng_core_nco.json","nuclearcraft");
        addModdedMixinCFG("mixins.novaeng_core_lootoverhaul.json","lootoverhaul");
        addModdedMixinCFG("mixins.novaeng_core_fluxnetworks.json","fluxnetworks");
        addModdedMixinCFG("mixins.novaeng_core_extrabotany.json","extrabotany");
        addModdedMixinCFG("mixins.novaeng_core_packagedauto.json","packagedauto");
        addModdedMixinCFG("mixins.novaeng_core_rftools.json","rftools");
        addModdedMixinCFG("mixins.novaeng_core_advancedrocketry.json","advancedrocketry");

        addMixinCFG("mixins.novaeng_core_botania_r.json",() -> {
            ConfigManager.sync(MOD_ID, Config.Type.INSTANCE);
            return modLoaded("botania") && NovaEngCoreConfig.SERVER.bot;
        });

        addMixinCFG("mixins.novaeng_core_forge_late.json");
        addMixinCFG("mixins.novaeng_core_dme.json",                               
                () -> Loader.isModLoaded("deepmoblearning") && Loader.instance().getIndexedModList().get("deepmoblearning").getName().equals("DeepMobEvolution"));
    }

    @Override
    public List<String> getMixinConfigs() {
        return new ObjectArrayList<>(MIXIN_CONFIGS.keySet());
    }

    @Override
    public boolean shouldMixinConfigQueue(final String mixinConfig) {
        BooleanSupplier supplier = MIXIN_CONFIGS.get(mixinConfig);
        if (supplier == null) {
            LOG.warn(LOG_PREFIX + "Mixin config {} is not found in config map! It will never be loaded.", mixinConfig);
            return false;
        }
        return supplier.getAsBoolean();
    }

    private static boolean modLoaded(final String modID) {
        return Loader.isModLoaded(modID);
    }

    private static void addModdedMixinCFG(final String mixinConfig, final String modID) {
        MIXIN_CONFIGS.put(mixinConfig, () -> modLoaded(modID));
    }

    private static void addModdedMixinCFG(final String mixinConfig, final String modID, final String... modIDs) {
        MIXIN_CONFIGS.put(mixinConfig, () -> modLoaded(modID) && Arrays.stream(modIDs).allMatch(Loader::isModLoaded));
    }

    private static void addMixinCFG(final String mixinConfig) {
        MIXIN_CONFIGS.put(mixinConfig, () -> true);
    }

    private static void addMixinCFG(final String mixinConfig, final BooleanSupplier conditions) {
        MIXIN_CONFIGS.put(mixinConfig, conditions);
    }
}
