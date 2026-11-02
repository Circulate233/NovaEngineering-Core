package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockRedstoneWire;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockRedstoneWire.EnumAttachPosition.class, remap = false)
public abstract class MixinEnumAttachPosition {

    @Shadow
    @Final
    private static BlockRedstoneWire.EnumAttachPosition[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockRedstoneWire.EnumAttachPosition[] values() {
        return $VALUES;
    }
}
