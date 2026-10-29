package github.kasuminova.novaeng.mixin.enumcache.modularui;

import com.cleanroommc.modularui.utils.ImageUtil.ImageType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ImageType.class, remap = false)
public abstract class MixinImageType {

    @Shadow
    @Final
    private static ImageType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ImageType[] values() {
        return $VALUES;
    }
}
