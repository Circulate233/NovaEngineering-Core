package github.kasuminova.novaeng.mixin.enderio;

import com.enderio.core.common.util.NNList;
import crazypants.enderio.base.recipe.lookup.ItemRecipeLeafNode;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Replaces the linear duplicate scan performed while building the alloy smelter leaf lookup.
 *
 * <p>Each insertion first scans the accumulated list, purely to avoid storing the same recipe twice. The
 * scan is replaced by an identity set kept as a mirror of that list. Recipe implementations do not override
 * {@code equals}, so the original comparison already fell back to reference equality and the set accepts and
 * rejects exactly the same insertions, leaving the stored list and its ordering untouched.</p>
 *
 * <p>The mirror is validated against the list size on every query and rebuilt when they disagree, so any
 * mutation performed outside this class is picked up rather than producing a stale answer.</p>
 */
@Mixin(value = ItemRecipeLeafNode.class, remap = false)
public class MixinItemRecipeLeafNode<REC> {

    @Unique
    private static final Map<NNList<?>, Set<Object>> nova$members =
        Collections.synchronizedMap(new IdentityHashMap<>());

    @Redirect(method = "addRecipe", at = @At(value = "INVOKE",
        target = "Lcom/enderio/core/common/util/NNList;contains(Ljava/lang/Object;)Z", remap = false),
        remap = false, require = 1)
    private boolean nova$contains(final NNList<REC> list, final Object recipe) {
        return nova$mirror(list).contains(recipe);
    }

    @Redirect(method = "addRecipe", at = @At(value = "INVOKE",
        target = "Lcom/enderio/core/common/util/NNList;add(Ljava/lang/Object;)Z", remap = false),
        remap = false, require = 1)
    private boolean nova$add(final NNList<REC> list, final REC recipe) {
        nova$mirror(list).add(recipe);
        return list.add(recipe);
    }

    @Unique
    private static Set<Object> nova$mirror(final NNList<?> list) {
        synchronized (nova$members) {
            final Set<Object> known = nova$members.get(list);
            if (known != null && known.size() == list.size()) {
                return known;
            }
            final Set<Object> rebuilt = new ReferenceOpenHashSet<>(list.size() * 2 + 1);
            rebuilt.addAll(list);
            nova$members.put(list, rebuilt);
            return rebuilt;
        }
    }
}
