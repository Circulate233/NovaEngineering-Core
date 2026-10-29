package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.util.PriorityExecutor.Priority;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Priority.class, remap = false)
public abstract class MixinPriority {

    @Shadow
    @Final
    private static Priority[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Priority[] values() {
        return $VALUES;
    }
}
