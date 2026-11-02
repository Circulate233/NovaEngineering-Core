package github.kasuminova.novaeng.mixin.enumcache.futuremc;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import thedarkcolour.futuremc.block.villagepillage.BlockGrindstone.EnumAttachment;

@Mixin(value = EnumAttachment.class, remap = false)
public abstract class MixinEnumAttachment {

    @Shadow
    @Final
    private static EnumAttachment[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumAttachment[] values() {
        return $VALUES;
    }
}
