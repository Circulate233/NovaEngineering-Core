package github.kasuminova.novaeng.mixin.librarianlib;

import com.teamwizardry.librarianlib.features.network.PacketHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = PacketHandler.class, remap = false)
public abstract class MixinLibrarianPacketPrewarm {
    @Redirect(method = "register", at = @At(value = "INVOKE",
        target = "Lcom/teamwizardry/librarianlib/features/saving/AbstractSaveHandler;cacheFields(Ljava/lang/Class;)V"), require = 1)
    private static void nova$deferPacketFields(final Class<?> clazz) {
        // The first serialization obtains its table from the guarded SavingFieldCache.
        // Packet discriminators and network registration continue at the original call site.
    }
}
