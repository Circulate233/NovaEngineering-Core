package github.kasuminova.novaeng.mixin.enumcache.mets;

import net.lrsoft.mets.block.MetsBlockWithTileEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MetsBlockWithTileEntity.class, remap = false)
public abstract class MixinMetsBlockWithTileEntity {

    @Shadow
    @Final
    private static MetsBlockWithTileEntity[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MetsBlockWithTileEntity[] values() {
        return $VALUES;
    }
}
