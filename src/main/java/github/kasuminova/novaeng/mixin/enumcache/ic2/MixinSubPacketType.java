package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.network.SubPacketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SubPacketType.class, remap = false)
public abstract class MixinSubPacketType {

    @Shadow
    @Final
    private static SubPacketType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SubPacketType[] values() {
        return $VALUES;
    }
}
