package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.item.ItemAtomicDisassembler.DisassemblerMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DisassemblerMode.class, remap = false)
public abstract class MixinDisassemblerMode {

    @Shadow
    @Final
    private static DisassemblerMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DisassemblerMode[] values() {
        return $VALUES;
    }
}
