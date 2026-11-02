package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.network.EnumPacketDirection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumPacketDirection.class, remap = false)
public abstract class MixinEnumPacketDirection {

    @Shadow
    @Final
    private static EnumPacketDirection[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumPacketDirection[] values() {
        return $VALUES;
    }
}
