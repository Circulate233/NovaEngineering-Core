package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.gui.toasts.TutorialToast;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TutorialToast.Icons.class, remap = false)
public abstract class MixinIcons {

    @Shadow
    @Final
    private static TutorialToast.Icons[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TutorialToast.Icons[] values() {
        return $VALUES;
    }
}
