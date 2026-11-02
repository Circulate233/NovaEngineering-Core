package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.block.transport.items.PipeSize;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PipeSize.class, remap = false)
public abstract class MixinPipeSize {

    @Shadow
    @Final
    private static PipeSize[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PipeSize[] values() {
        return $VALUES;
    }
}
