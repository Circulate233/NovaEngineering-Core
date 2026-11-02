package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.block.machine.tileentity.TileEntityReplicator.Mode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Mode.class, remap = false)
public abstract class MixinTileEntityReplicatorMode {

    @Shadow
    @Final
    private static Mode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Mode[] values() {
        return $VALUES;
    }
}
