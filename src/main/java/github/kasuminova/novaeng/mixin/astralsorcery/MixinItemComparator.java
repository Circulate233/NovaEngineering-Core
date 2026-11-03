package github.kasuminova.novaeng.mixin.astralsorcery;

import github.kasuminova.novaeng.common.performance.AstralItemComparison;
import hellfirepvp.astralsorcery.common.util.ItemComparator;
import hellfirepvp.astralsorcery.common.util.ItemComparator.Clause;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ItemComparator.class, remap = false)
public abstract class MixinItemComparator {
    /**
     * @author circulation
     * @reason 用局部位掩码去重条件，避免每次比较创建 HashSet 及其节点。
     */
    @Overwrite
    public static boolean compare(final ItemStack first, final ItemStack second, final Clause... clauses) {
        int mask = 0;
        for (final Clause clause : clauses) {
            if (clause != null) mask |= 1 << clause.ordinal();
        }
        return AstralItemComparison.compare(first, second, mask);
    }
}
