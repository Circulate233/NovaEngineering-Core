package github.kasuminova.novaeng.common.performance;

/**
 * Releases requirement copies which a returned MMCE context cannot reuse on its next init.
 */
public interface RecipeContextAccess {
    void nova$discardIdleRequirements();
}
