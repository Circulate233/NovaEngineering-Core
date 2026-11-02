package github.kasuminova.novaeng.mixin.cofh;

import cofh.cofhworld.util.random.WeightedBlock;
import github.kasuminova.novaeng.common.performance.WeightedBlockPredicate;
import net.minecraft.block.Block;
import net.minecraft.block.state.pattern.BlockMatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = WeightedBlock.class, remap = false)
public abstract class MixinWeightedBlockPredicate implements WeightedBlockPredicate {

    @Shadow
    @Final
    public Block block;

    @Unique
    private volatile BlockMatcher nova$matcher;

    @Override
    public BlockMatcher nova$replacementPredicate() {
        BlockMatcher matcher = this.nova$matcher;
        if (matcher == null) {
            matcher = BlockMatcher.forBlock(this.block);
            this.nova$matcher = matcher;
        }
        return matcher;
    }
}
