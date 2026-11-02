package github.kasuminova.novaeng.mixin.enumcache.futuremc;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import thedarkcolour.futuremc.block.villagepillage.BlockBell.BellAttachment;

@Mixin(value = BellAttachment.class, remap = false)
public abstract class MixinBellAttachment {

    @Shadow
    @Final
    private static BellAttachment[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BellAttachment[] values() {
        return $VALUES;
    }
}
