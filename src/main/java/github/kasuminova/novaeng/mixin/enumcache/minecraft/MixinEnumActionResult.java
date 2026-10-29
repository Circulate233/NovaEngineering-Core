package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.EnumActionResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumActionResult.class, remap = false)
public abstract class MixinEnumActionResult {

    @Shadow
    @Final
    private static EnumActionResult[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumActionResult[] values() {
        return $VALUES;
    }
}
