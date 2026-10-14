package github.kasuminova.novaeng.mixin.minecraft;

import com.google.common.base.Predicate;
import github.kasuminova.novaeng.client.model.BlockStatePredicateCache;
import github.kasuminova.novaeng.client.model.PredicateCache;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.multipart.ConditionAnd;
import net.minecraft.client.renderer.block.model.multipart.ICondition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(ConditionAnd.class)
public abstract class MixinConditionAndCanonicalization {
    @Shadow
    @Final
    private Iterable<ICondition> conditions;

    @Unique
    private final BlockStatePredicateCache nova$predicateCache = new BlockStatePredicateCache();

    /**
     * @author NovaEngineering contributors
     * @reason Cache and canonicalise conjunction predicates for each block state container.
     */
    @Overwrite
    public Predicate<IBlockState> getPredicate(final BlockStateContainer blockState) {
        return nova$predicateCache.getOrCompute(blockState, () -> {
            final List<Predicate<IBlockState>> predicates = new ObjectArrayList<>();
            for (ICondition condition : conditions) {
                if (condition != null) {
                    predicates.add(condition.getPredicate(blockState));
                }
            }
            return PredicateCache.and(predicates);
        });
    }
}
