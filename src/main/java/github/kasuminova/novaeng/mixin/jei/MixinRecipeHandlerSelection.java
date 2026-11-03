package github.kasuminova.novaeng.mixin.jei;

import mezz.jei.api.recipe.IRecipeHandler;
import mezz.jei.recipes.RecipeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Mixin(value = RecipeRegistry.class, remap = false)
public abstract class MixinRecipeHandlerSelection {

    @Unique
    private static final Object NOVA$ABSENT = new Object();

    @Unique
    private volatile ConcurrentMap<Object, ConcurrentMap<Class<?>, Object>> nova$handlers;

    @Shadow
    @Nullable
    private <T> IRecipeHandler<T> getRecipeHandler(final Class<? extends T> recipeClass,
                                                   @Nullable final String recipeCategoryUid) {
        throw new AssertionError();
    }

    @Redirect(method = {
        "addRecipe(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;)V",
        "getRecipeWrapper(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;)Lmezz/jei/api/recipe/IRecipeWrapper;",
        "getRecipeHandler(Ljava/lang/Class;)Lmezz/jei/api/recipe/IRecipeHandler;"
    }, at = @At(value = "INVOKE",
        target = "Lmezz/jei/recipes/RecipeRegistry;getRecipeHandler(Ljava/lang/Class;Ljava/lang/String;)Lmezz/jei/api/recipe/IRecipeHandler;"),
        require = 3)
    @Nullable
    @SuppressWarnings("unchecked")
    private <T> IRecipeHandler<T> nova$cachedHandler(final RecipeRegistry registry,
                                                     final Class<? extends T> recipeClass,
                                                     @Nullable final String recipeCategoryUid) {
        if (recipeClass == null) {
            return this.getRecipeHandler(recipeClass, recipeCategoryUid);
        }
        // HEI freezes both handler lists before adding recipes in its constructor. Initialize lazily:
        // these calls can precede constructor-tail Mixin initializers. Public lookups can be concurrent.
        ConcurrentMap<Object, ConcurrentMap<Class<?>, Object>> handlers = this.nova$handlers;
        if (handlers == null) {
            synchronized (this) {
                handlers = this.nova$handlers;
                if (handlers == null) {
                    this.nova$handlers = handlers = new ConcurrentHashMap<>();
                }
            }
        }
        final Object category = recipeCategoryUid == null ? NOVA$ABSENT : recipeCategoryUid;
        ConcurrentMap<Class<?>, Object> byClass = handlers.get(category);
        if (byClass == null) {
            byClass = new ConcurrentHashMap<>();
            final ConcurrentMap<Class<?>, Object> existing = handlers.putIfAbsent(category, byClass);
            if (existing != null) {
                byClass = existing;
            }
        }
        final Object cached = byClass.get(recipeClass);
        if (cached != null) {
            return cached == NOVA$ABSENT ? null : (IRecipeHandler<T>) cached;
        }
        // Keep the original exact/category/unsorted/assignable precedence, including missing handlers.
        final IRecipeHandler<T> handler = this.getRecipeHandler(recipeClass, recipeCategoryUid);
        byClass.putIfAbsent(recipeClass, handler == null ? NOVA$ABSENT : handler);
        return handler;
    }
}
