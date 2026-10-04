package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.google.common.base.Throwables;
import github.kasuminova.novaeng.common.util.NovaFastEventListener;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.IEventExceptionHandler;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Replaces {@code EventBus#post} with a dispatch loop that skips the cancellation checks a non-cancelable event cannot
 * act on.
 *
 * <p>The original walks the listener array and hands every listener to {@code ASMEventHandler#invoke}, which asks the
 * event whether it can be canceled and whether it already was, plus the annotation whether the listener receives
 * canceled events. Those are virtual calls on a call site shared by every event type in the game, so they cannot be
 * inlined, and on a busy tick they are asked tens of thousands of times. The post result is then derived from the same
 * two questions.</p>
 *
 * <p>This replacement keeps the loop's shape - the shutdown guard, the listener array lookup, exception handling with
 * the listener index and the post result - but picks the cheapest correct call per listener. When the event cannot be
 * canceled the listener always has to run, so listeners that understand this are invoked directly through
 * {@link NovaFastEventListener}; everything else, and every cancelable event, goes through {@code invoke} exactly as
 * before. Cancelable events keep the original checks, so their observable behavior is unchanged.</p>
 *
 * <p>Written as an {@code @Overwrite} rather than an injector so no {@code CallbackInfoReturnable} is allocated per
 * post: the bus is posted to tens of thousands of times per tick, and the overwhelming majority of those events cannot
 * be canceled.</p>
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

    /**
     * @author circulation
     * @reason Dispatch through each listener's fast path for events that cannot be canceled, and derive the result
     *         without the per-listener cancellation checks. Behavior is unchanged for cancelable events.
     */
    @Overwrite(remap = false)
    public boolean post(final Event event) {
        if (this.shutdown) {
            return false;
        }

        final IEventListener[] listeners = event.getListenerList().getListeners(this.busID);
        final boolean cancelable = event.isCancelable();
        int index = 0;
        try {
            if (cancelable) {
                for (; index < listeners.length; index++) {
                    listeners[index].invoke(event);
                }
            } else {
                for (; index < listeners.length; index++) {
                    final IEventListener listener = listeners[index];
                    if (listener instanceof NovaFastEventListener fast) {
                        fast.nova$invokeUncancellable(event);
                    } else {
                        listener.invoke(event);
                    }
                }
            }
        } catch (final Throwable throwable) {
            this.exceptionHandler.handleException((EventBus) (Object) this, event, listeners, index, throwable);
            Throwables.throwIfUnchecked(throwable);
            throw new RuntimeException(throwable);
        }

        return cancelable && event.isCanceled();
    }
}
