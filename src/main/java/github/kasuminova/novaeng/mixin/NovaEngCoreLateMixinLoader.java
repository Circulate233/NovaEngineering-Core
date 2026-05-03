package github.kasuminova.novaeng.mixin;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import net.minecraftforge.fml.common.Loader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

@SuppressWarnings({"unused", "SameParameterValue"})
public class NovaEngCoreLateMixinLoader implements IMixinConfigPlugin {

    private static final String MIXIN_ROOT = "github.kasuminova.novaeng.mixin.";

    @Override
    public void onLoad(final String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        String mixinName = mixinClassName;
        if (mixinName.startsWith(MIXIN_ROOT)) {
            mixinName = mixinName.substring(MIXIN_ROOT.length());
        }

        int split = mixinName.indexOf('.');
        if (split < 0) {
            return true;
        }

        String group = mixinName.substring(0, split);

        return switch (group) {
            case "dme" -> Loader.isModLoaded("deepmoblearning")
                && Loader.instance().getIndexedModList().get("deepmoblearning").getName().equals("DeepMobEvolution");
            case "botania_r" -> Loader.isModLoaded("botania") && NovaEngCoreConfig.SERVER.bot;
            case "ae2" -> Loader.isModLoaded("appliedenergistics2");
            case "ar" -> Loader.isModLoaded("advancedrocketry");
            case "astralsorcery" -> Loader.isModLoaded("astralsorcery");
            case "athenaeum" -> Loader.isModLoaded("athenaeum");
            case "betterp2p" -> Loader.isModLoaded("betterp2p");
            case "botania" -> Loader.isModLoaded("botania");
            case "codechickenlib" -> Loader.isModLoaded("codechickenlib");
            case "cofh" -> Loader.isModLoaded("cofhcore");
            case "draconicevolution" -> Loader.isModLoaded("draconicevolution");
            case "electroblobs" -> Loader.isModLoaded("ebwizardry");
            case "enderio" -> Loader.isModLoaded("enderio");
            case "extrabotany" -> Loader.isModLoaded("extrabotany");
            case "ic2" -> Loader.isModLoaded("ic2");
            case "immersiveengineering" -> Loader.isModLoaded("immersiveengineering");
            case "jei" -> Loader.isModLoaded("jei");
            case "jetif" -> Loader.isModLoaded("jetif");
            case "legendarytooltips" -> Loader.isModLoaded("legendarytooltips");
            case "libvulpes" -> Loader.isModLoaded("libvulpes");
            case "lootoverhaul" -> Loader.isModLoaded("lootoverhaul");
            case "mets" -> Loader.isModLoaded("mets");
            case "modularrouters" -> Loader.isModLoaded("modularrouters");
            case "nae2" -> Loader.isModLoaded("nae2");
            case "nco" -> Loader.isModLoaded("nuclearcraft");
            case "opticheck" -> Loader.isModLoaded("opticheck");
            case "packagedauto" -> Loader.isModLoaded("packagedauto");
            case "psi" -> Loader.isModLoaded("psi");
            case "rftools" -> Loader.isModLoaded("rftools");
            case "techguns" -> Loader.isModLoaded("techguns");
            case "threng" -> Loader.isModLoaded("threng");
            default -> true;
        };
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
