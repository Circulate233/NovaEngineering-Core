package github.kasuminova.novaeng.common.advancement;

/**
 * Bridge implemented on the vanilla inventory criterion; custom subclasses keep their own test method.
 */
public interface IndexedInventoryCriterion {
    boolean nova$needsFullSlotCount();

    boolean nova$testIndexed(InventoryEvaluation inventory);
}
