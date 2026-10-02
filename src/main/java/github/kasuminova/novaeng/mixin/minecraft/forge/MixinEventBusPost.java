package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.google.common.base.Throwables;
import github.kasuminova.novaeng.common.util.NovaFastEventListener;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.IEventExceptionHandler;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dispatches an event that cannot be canceled without asking every listener whether the event was canceled.
 *
 * <p>{@code EventBus#post} hands every listener to {@code ASMEventHandler#invoke}, which asks the event whether it can
 * be canceled and whether it already was, plus the annotation whether the listener receives canceled events. The two
 * event questions are virtual calls on a call site shared by every event type in the game, so they cannot be inlined,
 * and on a busy tick they are asked tens of thousands of times - most of the cost of the bus.</p>
 *
 * <p>When the event is not cancelable there is nothing to ask: the listener always has to run, so the loop is walked
 * here and listeners that understand this call the listener directly. Everything else about {@code post} is kept as it
 * was, including the exception handling with the listener index, the shutdown check and the return value, and events
 * that can be canceled are handed back to the original method untouched.</p>
 */
@Mixin(value = EventBus.class, remap = false)
public abstract class MixinEventBusPost {

    @Shadow
    @Final
    private int busID;

    @Shadow
    private IEventExceptionHandler exceptionHandler;

    @Shadow
    private boolean shutdown;

    @Inject(method = "post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z", at = @At("HEAD"), cancellable = true,
        remap = false)
    private void nova$postUncancellable(final Event event, final CallbackInfoReturnable<Boolean> cir) {
        if (this.shutdown || event.isCancelable()) {
            return;
        }

        final IEventListener[] listeners = event.getListenerList().getListeners(this.busID);
        int index = 0;
        try {
            for (; index < listeners.length; index++) {
                final IEventListener listener = listeners[index];
                if (listener instanceof NovaFastEventListener fast) {
                    fast.nova$invokeUncancellable(event);
                } else {
                    listener.invoke(event);
                }
            }
        } catch (final Throwable throwable) {
            this.exceptionHandler.handleException((EventBus) (Object) this, event, listeners, index, throwable);
            Throwables.throwIfUnchecked(throwable);
            throw new RuntimeException(throwable);
        }

        cir.setReturnValue(false);
    }
}
