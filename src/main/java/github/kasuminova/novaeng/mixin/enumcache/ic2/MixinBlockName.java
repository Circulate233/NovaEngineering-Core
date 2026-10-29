package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.ref.BlockName;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockName.class, remap = false)
public abstract class MixinBlockName {

    @Shadow
    @Final
    private static BlockName[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockName[] values() {
        return $VALUES;
    }
}
