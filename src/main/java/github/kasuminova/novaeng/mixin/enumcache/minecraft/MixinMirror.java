package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.Mirror;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Mirror.class, remap = false)
public abstract class MixinMirror {

    @Shadow
    @Final
    private static Mirror[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Mirror[] values() {
        return $VALUES;
    }
}
