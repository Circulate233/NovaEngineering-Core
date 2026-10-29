package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.common.model.animation.Clips;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Clips.CommonClipTypeAdapterFactory.class, remap = false)
public abstract class MixinCommonClipTypeAdapterFactory {

    @Shadow
    @Final
    private static Clips.CommonClipTypeAdapterFactory[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Clips.CommonClipTypeAdapterFactory[] values() {
        return $VALUES;
    }
}
