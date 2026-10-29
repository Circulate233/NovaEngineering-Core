package github.kasuminova.novaeng.mixin.enumcache.astralsorcery;

import hellfirepvp.astralsorcery.common.block.network.BlockCollectorCrystalBase.CollectorCrystalType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CollectorCrystalType.class, remap = false)
public abstract class MixinCollectorCrystalType {

    @Shadow
    @Final
    private static CollectorCrystalType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CollectorCrystalType[] values() {
        return $VALUES;
    }
}
