package github.kasuminova.novaeng.mixin.enumcache.tconstruct;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.gadgets.entity.EntityFancyItemFrame.FrameType;

@Mixin(value = FrameType.class, remap = false)
public abstract class MixinFrameType {

    @Shadow
    @Final
    private static FrameType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FrameType[] values() {
        return $VALUES;
    }
}
