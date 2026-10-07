package github.kasuminova.novaeng.mixin.actinium;

import dhj.embeddedt.embeddium.impl.render.chunk.compile.ChunkTaskOutput;
import dhj.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkJobResult;
import dhj.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkJobTyped;
import github.kasuminova.novaeng.client.memory.BudgetOutputAccess;
import github.kasuminova.novaeng.client.memory.BuildMemory;
import github.kasuminova.novaeng.client.memory.BuildOutputBytes;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

/** Uses fields on the existing job instead of allocating a lease or wrapping its consumer. */
@Mixin(value = ChunkJobTyped.class, remap = false)
public abstract class MixinChunkJobMemoryBudget {
    @Shadow @Final @Mutable private Consumer<?> consumer;
    @Unique private long nova$reservedBytes;
    @Unique private boolean nova$accounted;

    @Redirect(method = "<init>", at = @At(value = "FIELD",
        target = "Ldhj/embeddedt/embeddium/impl/render/chunk/compile/executor/ChunkJobTyped;consumer:Ljava/util/function/Consumer;",
        opcode = Opcodes.PUTFIELD), require = 1)
    private void nova$reserveJob(final ChunkJobTyped<?, ?> self, final Consumer<?> callback) {
        consumer = callback;
        nova$reservedBytes = BuildMemory.BUDGET.reserve();
        nova$accounted = true;
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE",
        target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"), require = 1)
    @SuppressWarnings("unchecked")
    private void nova$transferOutput(final Consumer<?> callback, final Object result) {
        ChunkTaskOutput output = null;
        if (nova$accounted) {
            long bytes = 0;
            try {
                if (result instanceof ChunkJobResult.Success<?> success && success.output() instanceof ChunkTaskOutput taskOutput) {
                    output = taskOutput;
                    bytes = BuildOutputBytes.count(output);
                }
            } finally {
                BuildMemory.BUDGET.complete(nova$reservedBytes, bytes);
                nova$accounted = false;
            }
            if (bytes != 0) {
                ((BudgetOutputAccess) output).nova$attachOutputBudget(BuildMemory.BUDGET, bytes);
            }
        }
        try {
            ((Consumer<Object>) callback).accept(result);
        } catch (final Throwable failure) {
            if (output != null) { ((BudgetOutputAccess) output).nova$releaseOutputBudget(); }
            throw failure;
        }
    }
}
