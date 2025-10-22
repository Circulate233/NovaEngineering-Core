package github.kasuminova.novaeng.common.tile.ecotech;

import javax.annotation.Nullable;

public interface EPart<C extends EPartController<?>> {

    @Nullable
    C getController();

    void setController(final EPartController<?> storageController);

    default boolean isAssembled() {
        return getController() != null;
    }

    void onAssembled();

    void onDisassembled();

}
