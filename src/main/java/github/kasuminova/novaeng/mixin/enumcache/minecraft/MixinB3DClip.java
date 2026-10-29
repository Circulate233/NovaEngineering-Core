package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.model.b3d.B3DClip;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = B3DClip.class, remap = false)
public abstract class MixinB3DClip {

    @Shadow
    @Final
    private static B3DClip[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static B3DClip[] values() {
        return $VALUES;
    }
}
