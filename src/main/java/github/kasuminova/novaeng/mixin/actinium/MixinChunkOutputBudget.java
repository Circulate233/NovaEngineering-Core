package github.kasuminova.novaeng.mixin.actinium;

import dhj.embeddedt.embeddium.impl.render.chunk.compile.ChunkTaskOutput;
import github.kasuminova.novaeng.client.memory.BudgetOutputAccess;
import github.kasuminova.novaeng.client.memory.BuildByteBudget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ChunkTaskOutput.class, remap = false)
public abstract class MixinChunkOutputBudget implements BudgetOutputAccess {
    @Unique private BuildByteBudget nova$budget;
    @Unique private long nova$budgetBytes;

    @Override
    public synchronized void nova$attachOutputBudget(final BuildByteBudget budget, final long bytes) {
        if (nova$budget != null) { throw new IllegalStateException("Chunk output already owns a budget"); }
        nova$budget = budget;
        nova$budgetBytes = bytes;
    }

    @Override
    public synchronized void nova$releaseOutputBudget() {
        if (nova$budget != null) {
            nova$budget.releaseOutput(nova$budgetBytes);
            nova$budget = null;
            nova$budgetBytes = 0;
        }
    }
}
