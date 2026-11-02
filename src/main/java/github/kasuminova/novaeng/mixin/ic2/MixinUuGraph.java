package github.kasuminova.novaeng.mixin.ic2;

import github.kasuminova.novaeng.common.util.UuGraphIterator;
import ic2.core.uu.LeanItemStack;
import ic2.core.uu.UuGraph;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
