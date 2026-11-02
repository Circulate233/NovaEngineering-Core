package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.entity.MoverType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MoverType.class, remap = false)
public abstract class MixinMoverType {

    @Shadow
    @Final
    private static MoverType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MoverType[] values() {
        return $VALUES;
    }
}
