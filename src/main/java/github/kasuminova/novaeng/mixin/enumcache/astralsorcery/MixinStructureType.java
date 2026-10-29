package github.kasuminova.novaeng.mixin.enumcache.astralsorcery;

import hellfirepvp.astralsorcery.common.data.world.data.StructureGenBuffer.StructureType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = StructureType.class, remap = false)
public abstract class MixinStructureType {

    @Shadow
    @Final
    private static StructureType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static StructureType[] values() {
        return $VALUES;
    }
}
