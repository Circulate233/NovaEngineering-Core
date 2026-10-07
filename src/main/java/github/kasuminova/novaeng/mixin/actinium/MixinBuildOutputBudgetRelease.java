package github.kasuminova.novaeng.mixin.actinium;

import dhj.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import dhj.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import github.kasuminova.novaeng.client.memory.BudgetOutputAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ChunkBuildOutput.class, remap = false)
public abstract class MixinBuildOutputBudgetRelease {
    @Redirect(method = "delete", at = @At(value = "INVOKE",
        target = "Ldhj/embeddedt/embeddium/impl/render/chunk/data/BuiltSectionMeshParts;free()V"), require = 1)
    private void nova$disposeBudget(final BuiltSectionMeshParts mesh) {
        try { mesh.free(); }
        finally { ((BudgetOutputAccess) this).nova$releaseOutputBudget(); }
    }
}
