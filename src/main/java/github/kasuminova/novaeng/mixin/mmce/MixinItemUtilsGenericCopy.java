package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.performance.mmce.GenericItemHandlerCopy;
import hellfirepvp.modularmachinery.common.util.IItemHandlerImpl;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ItemUtils.class, remap = false)
public abstract class MixinItemUtilsGenericCopy {
    @Shadow
    @Final
    private static ThreadLocal<?> COPY_SHELL_POOL;

    @Redirect(method = "copyItemHandlerComponents", at = @At(value = "NEW", target =
        "(Lnet/minecraftforge/items/IItemHandlerModifiable;)Lhellfirepvp/modularmachinery/common/util/IItemHandlerImpl;"),
        require = 1)
    private static IItemHandlerImpl nova$copyGenericHandler(final IItemHandlerModifiable source) {
        final InvokerItemHandlerCopyShellPool pool = (InvokerItemHandlerCopyShellPool) COPY_SHELL_POOL.get();
        if (!pool.nova$canBorrow()) return new IItemHandlerImpl(source);
        return ((GenericItemHandlerCopy) pool.nova$borrow()).nova$copyFromGeneric(source);
    }

    @Inject(method = "endHandlerCopySession", at = @At("HEAD"), require = 1)
    private static void nova$releaseGenericCopies(final CallbackInfo ci) {
        final InvokerItemHandlerCopyShellPool pool = (InvokerItemHandlerCopyShellPool) COPY_SHELL_POOL.get();
        if (pool.nova$depth() == 1) {
            final List<IItemHandlerImpl> leased = pool.nova$leased();
            for (int i = 0, size = leased.size(); i < size; i++)
                ((GenericItemHandlerCopy) leased.get(i)).nova$releaseGenericContents();
        }
    }
}
