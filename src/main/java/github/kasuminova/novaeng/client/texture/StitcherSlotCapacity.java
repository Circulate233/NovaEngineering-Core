package github.kasuminova.novaeng.client.texture;

/**
 * Exposes the lazily maintained free-capacity frontier of a Minecraft Stitcher slot.
 */
public interface StitcherSlotCapacity {

    /**
     * Returns the current subtree frontier, rebuilding it from immediate children when necessary.
     *
     * @return the exact non-dominated free capacities below this slot
     */
    SlotCapacityFrontier nova$getCapacityFrontier();
}
