package github.kasuminova.novaeng.mixin.enumcache.draconicevolution;

import com.brandon3055.draconicevolution.blocks.tileentity.TileStabilizedSpawner.SpawnerTier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SpawnerTier.class, remap = false)
public abstract class MixinSpawnerTier {

    @Shadow
    @Final
    private static SpawnerTier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SpawnerTier[] values() {
        return $VALUES;
    }
}
