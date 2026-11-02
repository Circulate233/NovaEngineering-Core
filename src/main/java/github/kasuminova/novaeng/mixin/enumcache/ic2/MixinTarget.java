package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.item.tool.ItemSprayer.Target;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Target.class, remap = false)
public abstract class MixinTarget {

    @Shadow
    @Final
    private static Target[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Target[] values() {
        return $VALUES;
    }
}
