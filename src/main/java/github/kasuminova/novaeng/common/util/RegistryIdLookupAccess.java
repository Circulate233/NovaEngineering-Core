package github.kasuminova.novaeng.common.util;

public interface RegistryIdLookupAccess {
    Object nova$getIndexedId(int id);

    default long nova$getRegistryRevision() {
        return -1;
    }
}
