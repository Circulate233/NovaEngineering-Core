package github.kasuminova.novaeng.mixin.enumcache.enderutilities;

import fi.dy.masa.enderutilities.tileentity.TileEntityInserter.RedstoneMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RedstoneMode.class, remap = false)
public abstract class MixinTileEntityInserterRedstoneMode {

    @Shadow
    @Final
    private static RedstoneMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RedstoneMode[] values() {
        return $VALUES;
    }
}
