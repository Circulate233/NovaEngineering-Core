package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.gui.toasts.SystemToast;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SystemToast.Type.class, remap = false)
public abstract class MixinSystemToastType {

    @Shadow
    @Final
    private static SystemToast.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SystemToast.Type[] values() {
        return $VALUES;
    }
}
