package github.kasuminova.novaeng.common.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.NovaEngineeringCore;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.lang.reflect.Field;

/**
 * Lets {@code JarDiscovererCache} read back the jar discovery cache it writes.
 *
 * <p>VintageFix builds its Kryo instance with the default instantiator, which refuses classes without a no-arg
 * constructor. {@code ASMModParser} is one of those, so every launch logs
 * "Class cannot be created (missing no-arg constructor)" and then re-parses every mod jar with ASM. Objenesis is
 * the fallback that default expects, and VintageFix even bundles it.</p>
 *
 * <p>This cannot be a Mixin: the cache class is initialized inside VintageFix's own coremod tweak, and Mixin only
 * prepares its configs after that point, so any mixin on this class fails as "target was loaded too early" and
 * aborts the launch. The coremod injection below runs before that tweak instead, so the instance can be corrected
 * while the class initializes.</p>
 */
public final class VintageFixJarCacheFix {

    private static final String CACHE_CLASS = "org.embeddedt.vintagefix.jarcache.JarDiscovererCache";
    private static final String KRYO_FIELD = "kryo";

    private VintageFixJarCacheFix() {
    }

    /**
     * Corrects the Kryo instance VintageFix will deserialize with. Must run before VintageFix's coremod tweak
     * calls {@code JarDiscovererCache.load()}; a no-op when the class is already loaded or anything is missing.
     */
    public static void apply() {
        if (!NovaEngCoreConfig.CLIENT.optimizeVintageFixJarCache) {
            return;
        }
        try {
            final Class<?> cacheClass = Class.forName(CACHE_CLASS, true, VintageFixJarCacheFix.class.getClassLoader());
            final Field kryoField = cacheClass.getDeclaredField(KRYO_FIELD);
            kryoField.setAccessible(true);
            final Kryo kryo = (Kryo) kryoField.get(null);
            if (kryo == null) {
                return;
            }
            kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        } catch (ClassNotFoundException | NoSuchFieldException ignored) {
            // VintageFix absent or refactored: nothing to correct.
        } catch (Throwable failure) {
            NovaEngineeringCore.log.warn("Unable to enable constructorless deserialization for VintageFix's "
                + "jar discoverer cache; it stays write-only and mods are rescanned", failure);
        }
    }
}
