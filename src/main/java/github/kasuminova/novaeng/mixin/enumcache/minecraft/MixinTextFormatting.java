package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TextFormatting.class, remap = false)
public abstract class MixinTextFormatting {

    @Shadow
    @Final
    private static TextFormatting[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TextFormatting[] values() {
        return $VALUES;
    }
}
