package github.kasuminova.novaeng.client.memory;

/** Transfers accounting from a job reservation to its output, then releases it once. */
public interface BudgetOutputAccess {
    void nova$attachOutputBudget(BuildByteBudget budget, long bytes);
    void nova$releaseOutputBudget();
}
