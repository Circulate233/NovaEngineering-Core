package github.kasuminova.novaeng.mixin.enderio;

import com.enderio.core.common.util.NNList;
import crazypants.enderio.base.recipe.lookup.ItemRecipeLeafNode;
import github.kasuminova.novaeng.common.util.NovaMirrorHolder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(value = ItemRecipeLeafNode.class, remap = false)
public class MixinItemRecipeLeafNode<REC> {

    @Redirect(method = "addRecipe", at = @At(value = "INVOKE",
        target = "Lcom/enderio/core/common/util/NNList;contains(Ljava/lang/Object;)Z", remap = false),
        remap = false, require = 1)
    private boolean nova$contains(final NNList<REC> list, final Object recipe) {
        final Set<Object> mirror = nova$mirror(list);
        return mirror != null ? mirror.contains(recipe) : list.contains(recipe);
    }

    @Redirect(method = "addRecipe", at = @At(value = "INVOKE",
        target = "Lcom/enderio/core/common/util/NNList;add(Ljava/lang/Object;)Z", remap = false),
        remap = false, require = 1)
    private boolean nova$add(final NNList<REC> list, final REC recipe) {
        final Set<Object> mirror = nova$mirror(list);
        final boolean added = list.add(recipe);
        if (added && mirror != null) {
            mirror.add(recipe);
        }
        return added;
    }

    @Unique
    private static Set<Object> nova$mirror(final NNList<?> list) {
        if (!(list instanceof NovaMirrorHolder holder)) {
            return null;
        }
        Set<Object> mirror = holder.nova$getMirror();
        if (mirror == null || mirror.size() != list.size()) {
            mirror = new ObjectOpenHashSet<>(list.size() * 2 + 1);
            mirror.addAll(list);
            holder.nova$setMirror(mirror);
        }
        return mirror;
    }
}
