package github.kasuminova.novaeng.mixin.ar;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;

import javax.annotation.Nullable;
import java.util.HashMap;

@Mixin(value = AtmosphereHandler.class, remap = false)
public abstract class MixinAtmosphereDimensionMap {

    @Unique
    private static final Int2ObjectMap<AtmosphereHandler> nova$dimensionOxygen = new Int2ObjectOpenHashMap<>(4);
    @Shadow
    private static HashMap<Integer, AtmosphereHandler> dimensionOxygen;

    @Inject(method = "<clinit>()V", at = @At("RETURN"), require = 1)
    private static void nova$releaseUnusedMap(final CallbackInfo ci) {
        dimensionOxygen = null;
    }

    @Invoker("<init>")
    public static AtmosphereHandler nova$createHandler(final int dimension) {
        throw new AssertionError();
    }

    /**
     * @author circulation
     * @reason Preserve live configuration checks and event registration with a primitive-key handler map.
     */
    @Overwrite
    public static void registerWorld(final int dimension) {
        final DimensionProperties properties = DimensionManager.getInstance().getDimensionProperties(dimension);
        if (ARConfiguration.getCurrentConfig().enableOxygen && properties.hasSurface()
            && (ARConfiguration.getCurrentConfig().overrideGCAir || dimension != ARConfiguration.getCurrentConfig().MoonId
            || properties.isNativeDimension)) {
            nova$dimensionOxygen.put(dimension, nova$createHandler(dimension));
            MinecraftForge.EVENT_BUS.register(nova$dimensionOxygen.get(dimension));
        }
    }

    /**
     * @author circulation
     * @reason Remove the primitive dimension key before preserving the original event-unregistration checks.
     */
    @Overwrite
    public static void unregisterWorld(final int dimension) {
        final AtmosphereHandler handler = nova$dimensionOxygen.remove(dimension);
        if (ARConfiguration.getCurrentConfig().enableOxygen && handler != null) {
            MinecraftForge.EVENT_BUS.unregister(handler);
            FMLCommonHandler.instance().bus().unregister(handler);
        }
    }

    /**
     * @author circulation
     * @reason Test a primitive dimension key without boxing.
     */
    @Overwrite
    public static boolean hasAtmosphereHandler(final int dimension) {
        return nova$dimensionOxygen.containsKey(dimension);
    }

    /**
     * @author circulation
     * @reason Resolve a primitive dimension key without boxing.
     */
    @Nullable
    @Overwrite
    public static AtmosphereHandler getOxygenHandler(final int dimension) {
        return nova$dimensionOxygen.get(dimension);
    }
}
