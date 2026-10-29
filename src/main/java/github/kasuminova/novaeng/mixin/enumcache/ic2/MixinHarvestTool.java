package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.ref.TeBlock.HarvestTool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = HarvestTool.class, remap = false)
public abstract class MixinHarvestTool {

    @Shadow
    @Final
    private static HarvestTool[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static HarvestTool[] values() {
        return $VALUES;
    }
}
