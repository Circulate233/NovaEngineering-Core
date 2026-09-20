package github.kasuminova.novaeng.common.blockstate;

/**
 * Marks a block-state implementation that supplies its own canonical property-transition lookup.
 *
 * <p>Minecraft normally constructs a second transition table for every state produced by a
 * {@code BlockStateContainer}. Implementations carrying this marker must provide their complete
 * property-transition behavior independently and must not read the vanilla table.</p>
 */
public interface PropertyTableIndependentState {
}
