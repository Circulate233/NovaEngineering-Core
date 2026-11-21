package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.string.StringCanon;
import net.minecraft.util.text.TextComponentString;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextComponentString.class)
public abstract class MixinTextComponentStringSharing {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static String nova$shareText(final String value) {
        return StringCanon.value(value);
    }
}
