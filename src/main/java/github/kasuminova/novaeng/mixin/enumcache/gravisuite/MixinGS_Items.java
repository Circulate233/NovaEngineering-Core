package github.kasuminova.novaeng.mixin.enumcache.gravisuite;

import com.chocohead.gravisuite.GS_Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GS_Items.class, remap = false)
public abstract class MixinGS_Items {

    @Shadow
    @Final
    private static GS_Items[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GS_Items[] values() {
        return $VALUES;
    }
}
