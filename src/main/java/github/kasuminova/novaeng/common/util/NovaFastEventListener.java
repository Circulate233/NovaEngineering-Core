package github.kasuminova.novaeng.common.util;

import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Implemented through a mixin by listeners that can be invoked without the per-listener cancellation checks.
 *
 * <p>Forge asks every listener whether the event is cancelable and whether it has already been canceled before calling
 * it. For an event that is not cancelable those two questions can only ever answer "go ahead", so a listener
 * implementing this interface can be invoked directly in that case; events that can be canceled keep the vanilla
 * checks.</p>
 */
public interface NovaFastEventListener {

    /** Invokes the listener for an event that cannot be canceled, so no cancellation check can apply to it. */
    void nova$invokeUncancellable(Event event);
}
