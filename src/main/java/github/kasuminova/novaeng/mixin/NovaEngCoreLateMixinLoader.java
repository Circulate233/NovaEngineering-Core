package github.kasuminova.novaeng.mixin;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import net.minecraftforge.fml.common.Loader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;
import static net.minecraftforge.fml.common.Loader.isModLoaded;

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
            case "dme" -> isModLoaded("deepmoblearning")
                && Loader.instance().getIndexedModList().get("deepmoblearning").getName().equals("DeepMobEvolution");
            case "botania_r" -> isModLoaded("botania") && NovaEngCoreConfig.SERVER.bot;
            case "ae2" -> isModLoaded("appliedenergistics2");
            case "ar" -> isModLoaded("advancedrocketry");
            case "actuallyadditions" -> isModLoaded("actuallyadditions");
            case "astralsorcery" -> isModLoaded("astralsorcery");
            case "athenaeum" -> isModLoaded("athenaeum");
            case "betterp2p" -> isModLoaded("betterp2p");
            case "botania" -> isModLoaded("botania");
            case "codechickenlib" -> isModLoaded("codechickenlib");
            case "cofh" -> isModLoaded("cofhcore");
            case "draconicevolution" -> isModLoaded("draconicevolution");
            case "electroblobs" -> isModLoaded("ebwizardry");
            case "enderio" -> isModLoaded("enderio");
            case "extrabotany" -> isModLoaded("extrabotany");
            case "ic2" -> isModLoaded("ic2");
            case "immersiveengineering" -> isModLoaded("immersiveengineering");
            case "jei" -> isModLoaded("jei");
            case "jetif" -> isModLoaded("jetif");
            case "legendarytooltips" -> isModLoaded("legendarytooltips");
            case "libvulpes" -> isModLoaded("libvulpes");
            case "lootoverhaul" -> isModLoaded("lootoverhaul");
            case "mets" -> isModLoaded("mets");
            case "modularrouters" -> isModLoaded("modularrouters");
            case "nae2" -> isModLoaded("nae2");
            case "nco" -> isModLoaded("nuclearcraft");
            case "opticheck" -> isModLoaded("opticheck");
            case "packagedauto" -> isModLoaded("packagedauto");
            case "psi" -> isModLoaded("psi");
            case "rftools" -> isModLoaded("rftools");
            case "techguns" -> isModLoaded("techguns");
            case "threng" -> isModLoaded("threng");
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
