package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.blocks.logic.sequencer.SequencerMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SequencerMode.class, remap = false)
public abstract class MixinSequencerMode {

    @Shadow
    @Final
    private static SequencerMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SequencerMode[] values() {
        return $VALUES;
    }
}
