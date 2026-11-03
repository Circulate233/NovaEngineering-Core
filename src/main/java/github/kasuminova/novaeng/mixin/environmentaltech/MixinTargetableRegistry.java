package github.kasuminova.novaeng.mixin.environmentaltech;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.valkyrieofnight.et.m_multiblocks.m_voidminer.registry.TargetableRegistry;
import com.valkyrieofnight.vliblegacy.lib.stack.WeightedStackBase;
import github.kasuminova.novaeng.common.performance.TargetedStackList;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(value = TargetableRegistry.class, remap = false)
public abstract class MixinTargetableRegistry {

    @Redirect(method = "getTargetedList", at = @At(value = "NEW", target = "java/util/ArrayList"), require = 1)
    private ArrayList<WeightedStackBase> nova$indexedWeights(final int initialCapacity) {
        return new TargetedStackList(initialCapacity);
    }

    @Redirect(method = "getTargetedList", at = @At(value = "INVOKE",
        target = "Ljava/util/ArrayList;iterator()Ljava/util/Iterator;"), require = 1)
    private Iterator<WeightedStackBase> nova$matchingWeights(final ArrayList<WeightedStackBase> weights,
                                                             @Local(name = "itemStack") final ItemStack itemStack,
                                                             @Local(name = "sedCo") final List<ItemStack> sedCo) {
        return weights instanceof TargetedStackList indexed
            ? indexed.matching(itemStack, sedCo.size()) : weights.iterator();
    }

    @ModifyReturnValue(method = "getTargetedList", at = @At("RETURN"), require = 1)
    private List<WeightedStackBase> nova$releaseIndex(final List<WeightedStackBase> weights) {
        if (weights instanceof TargetedStackList indexed) {
            indexed.finishBuilding();
        }
        return weights;
    }
}
