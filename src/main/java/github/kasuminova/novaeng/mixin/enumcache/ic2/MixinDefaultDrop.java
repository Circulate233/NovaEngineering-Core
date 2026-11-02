package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.ref.TeBlock.DefaultDrop;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DefaultDrop.class, remap = false)
public abstract class MixinDefaultDrop {

    @Shadow
    @Final
    private static DefaultDrop[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DefaultDrop[] values() {
        return $VALUES;
    }
}
