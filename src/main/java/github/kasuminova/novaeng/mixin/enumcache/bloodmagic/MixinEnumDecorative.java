package github.kasuminova.novaeng.mixin.enumcache.bloodmagic;

import WayofTime.bloodmagic.block.enums.EnumDecorative;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumDecorative.class, remap = false)
public abstract class MixinEnumDecorative {

    @Shadow
    @Final
    private static EnumDecorative[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumDecorative[] values() {
        return $VALUES;
    }
}
