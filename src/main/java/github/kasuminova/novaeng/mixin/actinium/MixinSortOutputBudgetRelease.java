package github.kasuminova.novaeng.mixin.actinium;

import dhj.embeddedt.embeddium.impl.common.util.NativeBuffer;
import dhj.embeddedt.embeddium.impl.render.chunk.compile.ChunkSortOutput;
import github.kasuminova.novaeng.client.memory.BudgetOutputAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ChunkSortOutput.class, remap = false)
public abstract class MixinSortOutputBudgetRelease {
    @Redirect(method = "delete", at = @At(value = "INVOKE",
        target = "Ldhj/embeddedt/embeddium/impl/common/util/NativeBuffer;free()V"), require = 1)
    private void nova$disposeBudget(final NativeBuffer buffer) {
        try { buffer.free(); }
        finally { ((BudgetOutputAccess) this).nova$releaseOutputBudget(); }
    }
}
