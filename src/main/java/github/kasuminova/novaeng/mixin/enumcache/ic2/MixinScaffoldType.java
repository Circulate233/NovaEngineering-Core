package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.block.BlockScaffold.ScaffoldType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ScaffoldType.class, remap = false)
public abstract class MixinScaffoldType {

    @Shadow
    @Final
    private static ScaffoldType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ScaffoldType[] values() {
        return $VALUES;
    }
}
