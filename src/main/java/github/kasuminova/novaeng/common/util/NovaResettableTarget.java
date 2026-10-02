package github.kasuminova.novaeng.common.util;

/**
 * Implemented through a mixin by objects whose per-use state can be cleared so the same instance can be handed out
 * again instead of being allocated once per use.
 */
public interface NovaResettableTarget {

    /** Clears everything the object accumulated in its previous use. */
    void nova$reset();

    /** Puts back the state the constructor of the concrete type would have set from its arguments. */
    void nova$setExtra(Object extra);
}
