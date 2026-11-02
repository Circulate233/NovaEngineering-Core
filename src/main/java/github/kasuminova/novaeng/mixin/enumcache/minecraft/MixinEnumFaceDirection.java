package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.renderer.EnumFaceDirection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumFaceDirection.class, remap = false)
public abstract class MixinEnumFaceDirection {

    @Shadow
    @Final
    private static EnumFaceDirection[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumFaceDirection[] values() {
        return $VALUES;
    }
}
