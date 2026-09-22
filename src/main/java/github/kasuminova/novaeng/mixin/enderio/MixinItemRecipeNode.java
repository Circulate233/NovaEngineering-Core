package github.kasuminova.novaeng.mixin.enderio;

import com.enderio.core.common.util.NNList;
import crazypants.enderio.base.recipe.lookup.ItemRecipeNode;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.Set;

/**
 * Replaces the linear duplicate scan performed while descending the alloy smelter lookup trie.
 *
 * <p>Each breadcrumb keeps a list of recipes and scans it before inserting, only to avoid duplicates. The
 * scan is replaced by an identity mirror of that list. Recipe implementations do not override
 * {@code equals}, so the original comparison already fell back to reference equality and the mirror accepts
 * and rejects exactly the same insertions, leaving every stored list and its ordering untouched.</p>
 *
 * <p>The mirror is validated against the list size on every query and rebuilt when they disagree, so any
 * mutation performed outside this class is picked up rather than producing a stale answer.</p>
 *
 * <p>The mirror map keys by reference and is only reached through the monitor taken below, so it needs no
 * wrapper of its own: a synchronized view would take the same lock a second time on every access.</p>
 */
@Mixin(value = ItemRecipeNode.class, remap = false)
public class MixinItemRecipeNode<REC> {

    @Unique
    private static final Map<NNList<?>, Set<Object>> nova$members = new Reference2ObjectOpenHashMap<>();

    @Redirect(method = "makeNext", at = @At(value = "INVOKE",
        target = "Lcom/enderio/core/common/util/NNList;contains(Ljava/lang/Object;)Z", remap = false),
        remap = false, require = 1)
    private boolean nova$contains(final NNList<REC> list, final Object recipe) {
        return nova$mirror(list).contains(recipe);
    }

    @Redirect(method = "makeNext", at = @At(value = "INVOKE",
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
