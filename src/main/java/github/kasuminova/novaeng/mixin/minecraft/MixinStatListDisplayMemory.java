package github.kasuminova.novaeng.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.common.performance.CompactStatName;
import net.minecraft.item.Item;
import net.minecraft.stats.StatCrafting;
import net.minecraft.stats.StatList;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StatList.class)
public abstract class MixinStatListDisplayMemory {
    @WrapOperation(method = {"initMiningStats", "initStats", "initItemDepleteStats", "initCraftableStats", "initPickedUpAndDroppedStats"},
        at = @At(value = "NEW", target = "net/minecraft/stats/StatCrafting"), require = 6)
    private static StatCrafting nova$compactOwnedDisplay(final String prefix, final String id,
                                                        final ITextComponent display, final Item item,
                                                        final Operation<StatCrafting> original) {
        return original.call(prefix, id, CompactStatName.capture(display), item);
    }
}
