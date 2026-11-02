package github.kasuminova.novaeng.mixin.ae2;

import appeng.services.compass.CompassReader;
import appeng.services.compass.CompassRegion;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.Map;

@Mixin(value = CompassReader.class, remap = false)
public abstract class MixinCompassReaderRegions {

    @Shadow
    @Final
    @Mutable
    private Map<Long, CompassRegion> regions;

    @Shadow
    @Final
    private int dimensionId;

    @Shadow
    @Final
    private File worldCompassFolder;

    @Inject(method = "<init>(ILjava/io/File;)V", at = @At("RETURN"), require = 1)
    private void nova$primitiveRegions(final int dimensionId, final File worldCompassFolder, final CallbackInfo ci) {
        final Long2ObjectMap<CompassRegion> primitive = new Long2ObjectOpenHashMap<>(Math.max(4, this.regions.size()));
        primitive.putAll(this.regions);
        this.regions = primitive;
    }

    /**
     * @author circulation
     * @reason Use the region coordinate directly as a long key without changing the original signed packing.
     */
    @Overwrite
    private CompassRegion getRegion(final int chunkX, final int chunkZ) {
        long key = chunkX >> 10;
        key <<= 32;
        key |= chunkZ >> 10;
        final Long2ObjectMap<CompassRegion> regions = (Long2ObjectMap<CompassRegion>) this.regions;
        CompassRegion region = regions.get(key);
        if (region == null) {
            region = new CompassRegion(chunkX, chunkZ, this.dimensionId, this.worldCompassFolder);
            regions.put(key, region);
        }
        return region;
    }
}
