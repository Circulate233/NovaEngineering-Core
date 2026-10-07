package github.kasuminova.novaeng.mixin.actinium;

import com.dhj.actinium.render.terrain.ActiniumWorldRenderer;
import com.dhj.actinium.render.terrain.TileEntityGlStateGuard;
import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.novaeng.client.render.TileEntityBatchScopes;
import github.kasuminova.novaeng.client.render.TileEntityBatchScopes.State;
import github.kasuminova.novaeng.client.render.TileEntityRenderCulling;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Opens Actinium's TESR GL guard and FastTESR batch only when the current render pass reaches an
 * actual dispatcher render call. The original pass predicate, iteration, exception handling, and
 * cleanup structure remain in control.
 */
@Mixin(value = ActiniumWorldRenderer.class, remap = false)
public abstract class MixinActiniumWorldRendererLazyBatch {

    @Unique
    private static void nova$restoreOpenedBatch() {
        final State state = TileEntityBatchScopes.current();
        if (state != null && state.isBatchOpened()) {
            TileEntityGlStateGuard.restoreForBatch();
        }
    }

    @Redirect(
        method = "renderBlockEntities(Lcom/dhj/actinium/render/terrain/ActiniumWorldRenderer$TileEntityRenderContext;)I",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;preDrawBatch()V",
            remap = true),
        remap = false,
        require = 1
    )
    private void nova$deferBatchOpen(final TileEntityRendererDispatcher dispatcher) {
    }

    @Unique
    private static void nova$drawOpenedBatch(final TileEntityRendererDispatcher dispatcher, final int renderPass) {
        final State state = TileEntityBatchScopes.current();
        if (state != null && state.isBatchOpened()) {
            dispatcher.drawBatch(renderPass);
        }
    }

    @Redirect(
        method = "renderBlockEntities(Lcom/dhj/actinium/render/terrain/ActiniumWorldRenderer$TileEntityRenderContext;)I",
        at = @At(value = "INVOKE",
            target = "Lcom/dhj/actinium/render/terrain/TileEntityGlStateGuard;restoreForBatch()V",
            ordinal = 0,
            remap = false),
        remap = false,
        require = 1
    )
    private void nova$restoreOpenedBatchNormal() {
        nova$restoreOpenedBatch();
    }

    @Redirect(
        method = "renderBlockEntities(Lcom/dhj/actinium/render/terrain/ActiniumWorldRenderer$TileEntityRenderContext;)I",
        at = @At(value = "INVOKE",
            target = "Lcom/dhj/actinium/render/terrain/TileEntityGlStateGuard;restoreForBatch()V",
            ordinal = 1,
            remap = false),
        remap = false,
        require = 1
    )
    private void nova$restoreOpenedBatchExceptional() {
        nova$restoreOpenedBatch();
    }

    @Redirect(
        method = "renderBlockEntities(Lcom/dhj/actinium/render/terrain/ActiniumWorldRenderer$TileEntityRenderContext;)I",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;drawBatch(I)V",
            ordinal = 0,
            remap = true),
        remap = false,
        require = 1
    )
    private void nova$drawOpenedBatchNormal(final TileEntityRendererDispatcher dispatcher, final int pass) {
        nova$drawOpenedBatch(dispatcher, pass);
    }

    @Redirect(
        method = "renderBlockEntities(Lcom/dhj/actinium/render/terrain/ActiniumWorldRenderer$TileEntityRenderContext;)I",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;drawBatch(I)V",
            ordinal = 1,
            remap = true),
        remap = false,
        require = 1
    )
    private void nova$drawOpenedBatchExceptional(final TileEntityRendererDispatcher dispatcher, final int pass) {
        nova$drawOpenedBatch(dispatcher, pass);
    }

    @Redirect(
        method = "renderBlockEntities(Lcom/dhj/actinium/render/terrain/ActiniumWorldRenderer$TileEntityRenderContext;)I",
        at = @At(value = "INVOKE",
            target = "Lcom/dhj/actinium/render/terrain/TileEntityGlStateGuard;pop()V",
            ordinal = 0,
            remap = false),
        remap = false,
        require = 1
    )
    private void nova$popOpenedGuardNormal() {
        nova$popOpenedGuard();
    }

    @Redirect(
        method = "renderBlockEntities(Lcom/dhj/actinium/render/terrain/ActiniumWorldRenderer$TileEntityRenderContext;)I",
        at = @At(value = "INVOKE",
            target = "Lcom/dhj/actinium/render/terrain/TileEntityGlStateGuard;pop()V",
            ordinal = 1,
            remap = false),
        remap = false,
        require = 1
    )
    private void nova$popOpenedGuardExceptional() {
        nova$popOpenedGuard();
    }

    @Unique
    private static void nova$popOpenedGuard() {
        final State state = TileEntityBatchScopes.current();
        try {
            if (state != null && state.isGuardPushed()) {
                TileEntityGlStateGuard.pop();
            }
        } finally {
            TileEntityBatchScopes.end();
        }
    }

    @Redirect(
        method = "renderBlockEntities(Lcom/dhj/actinium/render/terrain/ActiniumWorldRenderer$TileEntityRenderContext;)I",
        at = @At(value = "INVOKE",
            target = "Lcom/dhj/actinium/render/terrain/TileEntityGlStateGuard;push()V",
            remap = false),
        remap = false,
        require = 1
    )
    private void nova$deferGuardPush() {
        // The original guard push/pop already encloses the full method in a try/finally.
        TileEntityBatchScopes.begin();
    }

    @Redirect(
        method = "renderBlockEntityListInternal(Ljava/util/List;Lcom/dhj/actinium/render/terrain/ActiniumWorldRenderer$TileEntityRenderContext;Z)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;render(Lnet/minecraft/tileentity/TileEntity;FI)V",
            remap = true),
        remap = false,
        require = 1
    )
    private void nova$openBatchBeforeFirstRender(final TileEntityRendererDispatcher dispatcher,
                                                 final TileEntity tileentityIn,
                                                 final float partialTicks,
                                                 final int destroyStage,
                                                 @Local(argsOnly = true) final boolean globalRendererList) {
        // The section builder has already classified entries in the global list.
        if (!globalRendererList && TileEntityRenderCulling.shouldSkip(tileentityIn)) {
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
