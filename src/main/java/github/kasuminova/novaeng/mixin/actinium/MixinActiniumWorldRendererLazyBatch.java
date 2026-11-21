package github.kasuminova.novaeng.mixin.actinium;

import com.dhj.actinium.render.terrain.ActiniumWorldRenderer;
import com.dhj.actinium.render.terrain.TileEntityGlStateGuard;
import github.kasuminova.novaeng.client.render.TileEntityBatchScopes;
import github.kasuminova.novaeng.client.render.TileEntityBatchScopes.State;
import github.kasuminova.novaeng.client.render.TileEntityRenderCulling;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Opens the FastTESR batch when the current render pass reaches an actual dispatcher render call.
 * Actinium's GL guard still encloses its pass setup and all three TESR batchers' flushes.
 */
@Mixin(value = ActiniumWorldRenderer.class, remap = false)
public abstract class MixinActiniumWorldRendererLazyBatch {

    @Redirect(
        method = "renderBlockEntities(Lcom/dhj/actinium/render/terrain/ActiniumWorldRenderer$TileEntityRenderContext;)I",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;preDrawBatch()V",
            remap = false),
        remap = false,
        require = 1
    )
    private void nova$deferBatchOpen(final TileEntityRendererDispatcher dispatcher) {
    }

    @Redirect(
        method = "renderBlockEntities(Lcom/dhj/actinium/render/terrain/ActiniumWorldRenderer$TileEntityRenderContext;)I",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;drawBatch(I)V",
            remap = false),
        remap = false,
        require = 2
    )
    private void nova$drawOpenedBatchOnExit(final TileEntityRendererDispatcher dispatcher, final int pass) {
        final State state = TileEntityBatchScopes.current();
        if (state != null && state.isBatchOpened()) {
            dispatcher.drawBatch(pass);
        }
    }

    @Redirect(
        method = "renderBlockEntities(Lcom/dhj/actinium/render/terrain/ActiniumWorldRenderer$TileEntityRenderContext;)I",
        at = @At(value = "INVOKE",
            target = "Lcom/dhj/actinium/render/terrain/TileEntityGlStateGuard;pop()V",
            remap = false),
        remap = false,
        require = 2
    )
    private void nova$endBatchScope() {
        TileEntityBatchScopes.end();
        TileEntityGlStateGuard.pop();
    }

    @Redirect(
        method = "renderBlockEntities(Lcom/dhj/actinium/render/terrain/ActiniumWorldRenderer$TileEntityRenderContext;)I",
        at = @At(value = "INVOKE",
            target = "Lcom/dhj/actinium/render/terrain/TileEntityGlStateGuard;push()V",
            remap = false),
        remap = false,
        require = 1
    )
    private void nova$beginBatchScope() {
        // New TESR batchers may touch GL while beginning or flushing an otherwise empty pass.
        TileEntityGlStateGuard.push();
        TileEntityBatchScopes.begin();
    }

    @Redirect(
        method = "renderBlockEntityList(Ljava/util/List;Lcom/dhj/actinium/render/terrain/ActiniumWorldRenderer$TileEntityRenderContext;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;render(Lnet/minecraft/tileentity/TileEntity;FI)V",
            remap = true),
        remap = false,
        require = 1
    )
    private void nova$openBatchBeforeFirstRender(final TileEntityRendererDispatcher dispatcher,
                                                 final TileEntity tileentityIn,
                                                 final float partialTicks,
                                                 final int destroyStage) {
        if (TileEntityRenderCulling.shouldSkip(tileentityIn)) {
            return;
        }
        final State state = TileEntityBatchScopes.current();
        if (state != null) {
            state.ensureOpened(dispatcher);
        }
        final TileEntityRenderCulling.Approval previous = TileEntityRenderCulling.approve(tileentityIn);
        try {
            dispatcher.render(tileentityIn, partialTicks, destroyStage);
        } finally {
            TileEntityRenderCulling.restoreApproval(previous);
        }
    }

}
