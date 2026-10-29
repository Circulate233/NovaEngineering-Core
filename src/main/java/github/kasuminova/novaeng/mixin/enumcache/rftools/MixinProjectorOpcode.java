package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.blocks.shaper.ProjectorOpcode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ProjectorOpcode.class, remap = false)
public abstract class MixinProjectorOpcode {

    @Shadow
    @Final
    private static ProjectorOpcode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ProjectorOpcode[] values() {
        return $VALUES;
    }
}
