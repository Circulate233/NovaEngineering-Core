package github.kasuminova.novaeng.mixin.librarianlib;

import com.teamwizardry.librarianlib.features.autoregister.builtin.TileRegisterProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TileRegisterProcessor.class, remap = false)
public abstract class MixinLibrarianTilePrewarm {
    @Redirect(method = "process(Ljava/lang/Class;Lcom/teamwizardry/librarianlib/features/autoregister/TileRegister;)V",
        at = @At(value = "INVOKE",
            target = "Lcom/teamwizardry/librarianlib/features/saving/AbstractSaveHandler;cacheFields(Ljava/lang/Class;)V"),
        require = 1)
    private void nova$deferTileFields(final Class<?> clazz) {
        // Preserve tile registration; serialization/capability access initializes the fields when used.
    }
}
