package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.border.EnumBorderStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumBorderStatus.class, remap = false)
public abstract class MixinEnumBorderStatus {

    @Shadow
    @Final
    private static EnumBorderStatus[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumBorderStatus[] values() {
        return $VALUES;
    }
}
