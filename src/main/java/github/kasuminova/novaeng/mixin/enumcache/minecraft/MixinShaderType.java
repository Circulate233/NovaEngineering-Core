package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.shader.ShaderLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ShaderLoader.ShaderType.class, remap = false)
public abstract class MixinShaderType {

    @Shadow
    @Final
    private static ShaderLoader.ShaderType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ShaderLoader.ShaderType[] values() {
        return $VALUES;
    }
}
