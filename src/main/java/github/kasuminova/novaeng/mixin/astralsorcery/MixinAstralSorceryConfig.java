package github.kasuminova.novaeng.mixin.astralsorcery;

import hellfirepvp.astralsorcery.common.data.config.Config;
import net.minecraftforge.common.config.Configuration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Config.class, remap = false)
public abstract class MixinAstralSorceryConfig {

    @Redirect(
        method = "attemptLoad",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/config/Configuration;save()V",
            remap = false),
        require = 1)
    private static void nova$saveIfChanged(final Configuration configuration) {
        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
