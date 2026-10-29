package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.EnumParticleTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumParticleTypes.class, remap = false)
public abstract class MixinEnumParticleTypes {

    @Shadow
    @Final
    private static EnumParticleTypes[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumParticleTypes[] values() {
        return $VALUES;
    }
}
