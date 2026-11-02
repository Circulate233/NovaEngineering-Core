package github.kasuminova.novaeng.mixin.enumcache.gravisuite;

import com.chocohead.gravisuite.items.ItemAdvancedDrill.DrillMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DrillMode.class, remap = false)
public abstract class MixinDrillMode {

    @Shadow
    @Final
    private static DrillMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DrillMode[] values() {
        return $VALUES;
    }
}
