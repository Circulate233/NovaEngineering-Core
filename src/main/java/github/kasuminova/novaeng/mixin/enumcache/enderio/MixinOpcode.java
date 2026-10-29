package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.filter.network.PacketExistingItemFilterSnapshot.Opcode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Opcode.class, remap = false)
public abstract class MixinOpcode {

    @Shadow
    @Final
    private static Opcode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Opcode[] values() {
        return $VALUES;
    }
}
