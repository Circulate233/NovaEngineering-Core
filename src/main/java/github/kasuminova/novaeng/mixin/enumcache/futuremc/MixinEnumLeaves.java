package github.kasuminova.novaeng.mixin.enumcache.futuremc;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import thedarkcolour.futuremc.block.villagepillage.BlockBamboo.EnumLeaves;

@Mixin(value = EnumLeaves.class, remap = false)
public abstract class MixinEnumLeaves {

    @Shadow
    @Final
    private static EnumLeaves[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumLeaves[] values() {
        return $VALUES;
    }
}
