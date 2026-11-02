package github.kasuminova.novaeng.mixin.enumcache.gravisuite;

import com.chocohead.gravisuite.items.ItemGraviTool.GraviToolMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GraviToolMode.class, remap = false)
public abstract class MixinGraviToolMode {

    @Shadow
    @Final
    private static GraviToolMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GraviToolMode[] values() {
        return $VALUES;
    }
}
