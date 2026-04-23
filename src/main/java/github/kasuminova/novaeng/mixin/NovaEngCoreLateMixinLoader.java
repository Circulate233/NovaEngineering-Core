package github.kasuminova.novaeng.mixin;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.Loader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static github.kasuminova.novaeng.NovaEngineeringCore.MOD_ID;
import static github.kasuminova.novaeng.mixin.NovaEngCoreEarlyMixinLoader.LOG;
import static github.kasuminova.novaeng.mixin.NovaEngCoreEarlyMixinLoader.LOG_PREFIX;

@SuppressWarnings({"unused", "SameParameterValue"})
public class NovaEngCoreLateMixinLoader implements IMixinConfigPlugin {

    private static final Map<String, BooleanSupplier> MIXIN_PACKAGES = new Object2ObjectLinkedOpenHashMap<>();

    static {
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.ae2", "appliedenergistics2");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.ar", "advancedrocketry");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.astralsorcery", "astralsorcery");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.athenaeum", "athenaeum");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.betterp2p", "betterp2p");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.botania", "botania", "psi");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.codechickenlib", "codechickenlib");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.cofh", "cofhcore");
        addMixinPackage("github.kasuminova.novaeng.mixin.dme",
            () -> Loader.isModLoaded("deepmoblearning") && Loader.instance().getIndexedModList().get("deepmoblearning").getName().equals("DeepMobEvolution"));
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.draconicevolution", "draconicevolution");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.electroblobs", "ebwizardry");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.enderio", "enderio");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.extrabotany", "extrabotany");
        addMixinPackage("github.kasuminova.novaeng.mixin.minecraft.forge");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.ic2", "ic2");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.immersiveengineering", "immersiveengineering");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.jei", "jei");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.jetif", "jetif");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.legendarytooltips", "legendarytooltips");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.libvulpes", "libvulpes");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.lootoverhaul", "lootoverhaul");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.mets", "mets");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.modularrouters", "modularrouters");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.nae2", "nae2");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.nco", "nuclearcraft");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.opticheck", "opticheck");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.packagedauto", "packagedauto");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.psi", "psi");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.rftools", "rftools");
        addModdedMixinPackage("github.kasuminova.novaeng.mixin.techguns", "techguns");

        addMixinPackage("github.kasuminova.novaeng.mixin.botania_r", () -> {
            ConfigManager.sync(MOD_ID, Config.Type.INSTANCE);
            return Loader.isModLoaded("botania") && NovaEngCoreConfig.SERVER.bot;
        });
    }

    private boolean shouldApply = true;

    private static void addModdedMixinPackage(final String mixinPackage, final String modID) {
        MIXIN_PACKAGES.put(mixinPackage, () -> Loader.isModLoaded(modID));
    }

    private static void addModdedMixinPackage(final String mixinPackage, final String modID, final String... modIDs) {
        MIXIN_PACKAGES.put(mixinPackage, () -> Loader.isModLoaded(modID) && java.util.Arrays.stream(modIDs).allMatch(Loader::isModLoaded));
    }

    private static void addMixinPackage(final String mixinPackage) {
        MIXIN_PACKAGES.put(mixinPackage, () -> true);
    }

    private static void addMixinPackage(final String mixinPackage, final BooleanSupplier conditions) {
        MIXIN_PACKAGES.put(mixinPackage, conditions);
    }

    @Override
    public void onLoad(final String mixinPackage) {
        BooleanSupplier supplier = MIXIN_PACKAGES.get(mixinPackage);
        if (supplier == null) {
            LOG.warn(LOG_PREFIX + "Mixin package {} is not found in config map. It will be applied by default.", mixinPackage);
            shouldApply = true;
            return;
        }
        shouldApply = supplier.getAsBoolean();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        return shouldApply;
    }

    @Override
    public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) {

    }
}
