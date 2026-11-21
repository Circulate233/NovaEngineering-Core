package github.kasuminova.novaeng;

import com.cleanroommc.discovery.CleanroomModDiscoverer;
import github.kasuminova.novaeng.common.asm.early.EarlyEventTransformer;
import github.kasuminova.novaeng.common.util.VintageFixJarCacheFix;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.outlands.foundation.TransformerDelegate;

import javax.annotation.Nullable;
import java.util.Map;

@SuppressWarnings("unused")
public class NovaEngCoreEarlyMixinLoader implements IFMLLoadingPlugin {
    public static final Logger LOG = LogManager.getLogger("NOVAENG_CORE_PRE");
    public static final String LOG_PREFIX = "[NOVAENG_CORE_PRE]" + ' ';

    public NovaEngCoreEarlyMixinLoader() {
        if (!Boolean.parseBoolean(System.getProperty("novaeng.earlyEventTransforms", "true"))) return;
        Launch.classLoader.addTransformerExclusion("github.kasuminova.novaeng.common.asm.early.");
        final EarlyEventTransformer transformer = new EarlyEventTransformer();
        for (final String target : new String[]{EarlyEventTransformer.SUBSCRIBER, EarlyEventTransformer.SUBSCRIPTION}) {
            if (Launch.classLoader.isClassLoaded(target)) {
                LOG.warn("{} was loaded before NovaCore's early event patch; keeping original behavior", target);
            } else {
                TransformerDelegate.registerExplicitTransformer(transformer, target);
            }
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(final Map<String, Object> data) {
        // Runs before VintageFix's tweak loads and deserializes its jar discovery cache.
        if ( CleanroomModDiscoverer.instance().isModPresent("vintagefix"))
            VintageFixJarCacheFix.apply();
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
