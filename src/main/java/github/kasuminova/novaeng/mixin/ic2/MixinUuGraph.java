package github.kasuminova.novaeng.mixin.ic2;

import github.kasuminova.novaeng.common.util.UuGraphIterator;
import github.kasuminova.novaeng.common.util.UuGraphNodeView;
import ic2.core.uu.LeanItemStack;
import ic2.core.uu.UuGraph;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.ObjDoubleConsumer;

@Mixin(value = UuGraph.class, remap = false)
public class MixinUuGraph implements UuGraphIterator {

    @Mutable
    @Shadow
    @Final
    private static Map<LeanItemStack, UuGraph.Node> nodes;

    @Mutable
    @Shadow
    @Final
    private static Map<Item, Set<UuGraph.Node>> itemNodes;

    @Shadow
    private static UuGraph.Node getInternal(LeanItemStack stack) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    /**
     * @author circulation
     * @reason Exact-query callers only read; reuse the normalized node's immutable singleton.
     */
    @Overwrite
    private static Collection<UuGraph.Node> getAll(LeanItemStack stack) {
        if (stack.getMeta() != 32767) {
            // IC2 hashes a normalized copy. Looking up the caller directly would cache its
            // NBT-dependent hash and make later queries stale if the caller mutates that NBT.
            return ((UuGraphNodeView) getInternal(stack)).nova$singleNodeView();
        }
        // Wildcards intentionally retain IC2's live item set, including its NBT semantics.
        Collection<UuGraph.Node> matches = itemNodes.get(stack.getItem());
        return matches != null ? matches : ObjectLists.emptyList();
    }

    @Shadow
    private static void finishCalculation() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void onInit(CallbackInfo ci) {
        nodes = new Object2ObjectOpenHashMap<>();
        itemNodes = new Reference2ObjectOpenHashMap<>();
    }

    @Override
    public void n$forEach(ObjDoubleConsumer<ItemStack> consumer) {
        finishCalculation();
        Object2ObjectMaps.fastForEach((Object2ObjectMap<LeanItemStack, UuGraph.Node>) nodes,
            e -> {
                var node = e.getValue();
                consumer.accept(node.stack.toMcStack(), node.value);
            });
    }

}
