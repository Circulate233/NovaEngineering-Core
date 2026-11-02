package github.kasuminova.novaeng.mixin.ar;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.AreaBlob;
import zmaster587.advancedRocketry.api.IAtmosphere;
import zmaster587.advancedRocketry.api.util.IBlobHandler;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.libVulpes.util.HashedBlockPosition;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;

@Mixin(value = AtmosphereHandler.class, remap = false)
public abstract class MixinAtmosphereHandler {

    @Shadow
    private HashMap<IBlobHandler, AreaBlob> blobs;

    @Shadow
    @Nonnull
    public abstract IAtmosphere getDefaultAtmosphereType();

    /**
     * @author circulation
     * @reason Avoid coordinate wrappers and iterators when a dimension has no local atmosphere blobs.
     */
    @Nonnull
    @Overwrite
    public IAtmosphere getAtmosphereType(@Nonnull final BlockPos position) {
        if (!ARConfiguration.getCurrentConfig().enableOxygen) {
            return AtmosphereType.AIR;
        }
        if (!this.blobs.isEmpty()) {
            final HashedBlockPosition hashed = new HashedBlockPosition(position);
            for (final AreaBlob blob : this.blobs.values()) {
                if (blob.contains(hashed)) {
                    final IAtmosphere atmosphere = (IAtmosphere) blob.getData();
                    if (atmosphere != null) {
                        return atmosphere;
                    }
                }
            }
        }
        return this.getDefaultAtmosphereType();
    }

    /**
     * @author circulation
     * @reason Keep the mutable, ordered result without allocating a linked-list node for every nearby blob.
     */
    @Nonnull
    @Overwrite
    protected List<AreaBlob> getBlobWithinRadius(@Nonnull final HashedBlockPosition position, final int radius) {
        final ObjectArrayList<AreaBlob> nearby = new ObjectArrayList<>(this.blobs.size());
        if (!this.blobs.isEmpty()) {
            for (final AreaBlob blob : this.blobs.values()) {
                if (blob.getRootPosition().getDistance(position) - radius <= 0.0D) {
                    nearby.add(blob);
                }
            }
        }
        return nearby;
    }
}
