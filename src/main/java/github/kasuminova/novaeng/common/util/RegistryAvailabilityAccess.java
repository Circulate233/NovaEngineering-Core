package github.kasuminova.novaeng.common.util;

/**
 * Primitive cursor access for ForgeRegistry's anonymous iterator.
 */
public interface RegistryAvailabilityAccess {
    int nova$nextAvailableRegistryId(int from);
}
