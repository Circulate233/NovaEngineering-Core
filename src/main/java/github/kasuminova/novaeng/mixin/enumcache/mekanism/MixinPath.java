package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.content.transporter.TransporterStack.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Path.class, remap = false)
public abstract class MixinPath {

    @Shadow
    @Final
    private static Path[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Path[] values() {
        return $VALUES;
    }
}
