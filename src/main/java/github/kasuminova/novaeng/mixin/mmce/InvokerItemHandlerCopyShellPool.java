package github.kasuminova.novaeng.mixin.mmce;

import hellfirepvp.modularmachinery.common.util.IItemHandlerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Pseudo
@Mixin(targets = "hellfirepvp.modularmachinery.common.util.ItemUtils$CopyShellPool", remap = false)
public interface InvokerItemHandlerCopyShellPool {
    @Invoker("canBorrow")
    boolean nova$canBorrow();

    @Invoker("borrow")
    IItemHandlerImpl nova$borrow();

    @Accessor("depth")
    int nova$depth();

    @Accessor("leased")
    List<IItemHandlerImpl> nova$leased();
}
