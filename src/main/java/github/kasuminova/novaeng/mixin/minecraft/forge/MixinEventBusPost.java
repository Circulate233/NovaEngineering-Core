package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.common.util.NovaFastEventListener;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Uses the direct listener path for non-cancelable events without replacing EventBus.post.
 */
@Mixin(value = EventBus.class, remap = false)
public abstract class MixinEventBusPost {

    @Redirect(method = "post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/fml/common/eventhandler/IEventListener;invoke(Lnet/minecraftforge/fml/common/eventhandler/Event;)V"))
    private void nova$invokeListener(final IEventListener listener, final Event event) {
        // Most listeners are ordinary ASM handlers; avoid asking the event about cancellation twice for them.
        if (listener instanceof NovaFastEventListener fast && !event.isCancelable()) {
            fast.nova$invokeUncancellable(event);
        } else {
            listener.invoke(event);
        }
    }
}
