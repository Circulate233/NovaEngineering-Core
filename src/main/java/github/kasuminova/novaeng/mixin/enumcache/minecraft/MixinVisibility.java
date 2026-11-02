package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.gui.toasts.IToast;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = IToast.Visibility.class, remap = false)
public abstract class MixinVisibility {

    @Shadow
    @Final
    private static IToast.Visibility[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static IToast.Visibility[] values() {
        return $VALUES;
    }
}
