package github.kasuminova.novaeng.mixin.enumcache.bogosorter;

import com.cleanroommc.bogosorter.common.Align.Corner;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Corner.class, remap = false)
public abstract class MixinCorner {

    @Shadow
    @Final
    private static Corner[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Corner[] values() {
        return $VALUES;
    }
}
