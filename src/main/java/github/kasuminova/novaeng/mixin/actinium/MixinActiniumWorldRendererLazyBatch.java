package github.kasuminova.novaeng.mixin.actinium;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.dhj.actinium.render.terrain.ActiniumWorldRenderer;
import com.dhj.actinium.render.terrain.TileEntityGlStateGuard;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayDeque;

/**
 * Opens Actinium's TESR GL guard and FastTESR batch only when the current render pass reaches an
 * actual dispatcher render call. The original pass predicate, iteration, exception handling, and
 * cleanup structure remain in control.
 */
@Mixin(value = ActiniumWorldRenderer.class, remap = false)
public abstract class MixinActiniumWorldRendererLazyBatch {

    @Unique
    private static final ThreadLocal<ArrayDeque<BatchState>> nova$batchStates = new ThreadLocal<>();

    @WrapMethod(
        method = "renderBlockEntities(Lcom/dhj/actinium/render/terrain/ActiniumWorldRenderer$TileEntityRenderContext;)I",
        remap = false,
        require = 1
    )
    private int nova$withLazyBatchScope(@Coerce final Object tileEntityRenderContext, final Operation<Integer> original) {
        ArrayDeque<BatchState> states = nova$batchStates.get();
        if (states == null) {
            states = new ArrayDeque<>();
            nova$batchStates.set(states);
        }
        states.push(new BatchState());
        try {
            return original.call(tileEntityRenderContext);
        } finally {
            states.pop();
            if (states.isEmpty()) {
                nova$batchStates.remove();
            }
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

    @WrapOperation(
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
                                                 final int destroyStage,
                                                 final Operation<Void> original) {
        final BatchState state = nova$currentBatchState();
        if (state != null) {
            state.ensureOpened(dispatcher);
        }
        original.call(dispatcher, tileentityIn, partialTicks, destroyStage);
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
    private static BatchState nova$currentBatchState() {
        final ArrayDeque<BatchState> states = nova$batchStates.get();
        return states == null ? null : states.peek();
    }

    @Unique
    private static void nova$restoreOpenedBatch() {
        final BatchState state = nova$currentBatchState();
        if (state != null && state.batchOpened) {
            TileEntityGlStateGuard.restoreForBatch();
        }
    }

    @Unique
    private static void nova$drawOpenedBatch(final TileEntityRendererDispatcher dispatcher, final int renderPass) {
        final BatchState state = nova$currentBatchState();
        if (state != null && state.batchOpened) {
            dispatcher.drawBatch(renderPass);
        }
    }

    @Unique
    private static void nova$popOpenedGuard() {
        final BatchState state = nova$currentBatchState();
        if (state != null && state.guardPushed) {
            TileEntityGlStateGuard.pop();
        }
    }

    @Unique
    private static final class BatchState {
        private boolean guardPushed;
        private boolean batchOpened;

        private void ensureOpened(final TileEntityRendererDispatcher dispatcher) {
            if (!this.guardPushed) {
                TileEntityGlStateGuard.push();
                this.guardPushed = true;
            }
            if (!this.batchOpened) {
                dispatcher.preDrawBatch();
                this.batchOpened = true;
            }
        }
    }
}
