package github.kasuminova.novaeng.common.performance.ae2;

/**
 * An outer item-map hint, deliberately distinct from IItemList's variant count.
 */
public interface CraftingSnapshotCapacity {
    int nova$itemTypeCount();

    void nova$reserveItemTypes(int count);
}
