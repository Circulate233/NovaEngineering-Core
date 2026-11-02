package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.Rotation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Rotation.class, remap = false)
public abstract class MixinRotation {

    @Shadow
    @Final
    private static Rotation[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Rotation[] values() {
        return $VALUES;
    }
}
