package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumFacing.class, remap = false)
public abstract class MixinEnumFacing {

    @Shadow
    @Final
    private static EnumFacing[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumFacing[] values() {
        return $VALUES;
    }
}
