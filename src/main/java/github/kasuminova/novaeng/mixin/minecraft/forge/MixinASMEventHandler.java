package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.common.util.NovaFastEventListener;
import net.minecraftforge.fml.common.eventhandler.ASMEventHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Consumer;

/**
 * Lets a non-cancelable event reach the listener without the cancellation checks {@code invoke} performs.
 *
 * <p>{@code invoke} asks the event whether it is cancelable and whether it is already canceled, and the subscribe
 * annotation whether the listener wants canceled events, before calling the listener. For an event that cannot be
 * canceled the answers cannot change the outcome, so the listener is called directly - the only thing left to keep is
 * the mod logging context, which is why the original method is still used when that context is enabled.</p>
 */
@Mixin(value = ASMEventHandler.class, remap = false)
public abstract class MixinASMEventHandler implements NovaFastEventListener {

    @Shadow
    @Final
    private static boolean GETCONTEXT;

    @Shadow
    @Final
    private Consumer<Event> handler;

    @Shadow
    public abstract void invoke(Event event);

    @Override
    public void nova$invokeUncancellable(final Event event) {
        if (GETCONTEXT) {
            // The original method also fills in and clears the log context around the call.
            this.invoke(event);
            return;
        }
        final Consumer<Event> listener = this.handler;
        if (listener != null) {
            listener.accept(event);
        }
    }
}
