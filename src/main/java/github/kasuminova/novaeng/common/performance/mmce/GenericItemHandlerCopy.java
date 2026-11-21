package github.kasuminova.novaeng.common.performance.mmce;

import hellfirepvp.modularmachinery.common.util.IItemHandlerImpl;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * Refreshes a shell leased by MMCE's existing, thread-local handler-copy session.
 */
public interface GenericItemHandlerCopy {
    IItemHandlerImpl nova$copyFromGeneric(IItemHandlerModifiable source);

    void nova$releaseGenericContents();
}
