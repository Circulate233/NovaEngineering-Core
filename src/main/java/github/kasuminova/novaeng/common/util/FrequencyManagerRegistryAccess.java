package github.kasuminova.novaeng.common.util;

/**
 * Access used only after Mekanism has discarded its private managers and reset their world data.
 */
public interface FrequencyManagerRegistryAccess {
    void nova$unregisterDiscardedManager();
}
