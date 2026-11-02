package github.kasuminova.novaeng.mixin.cofh;

import cofh.cofhworld.util.random.WeightedBlock;
import cofh.cofhworld.world.generator.WorldGen;
import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.novaeng.common.performance.WeightedBlockPredicate;
import net.minecraft.block.Block;
import net.minecraft.block.state.pattern.BlockMatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WorldGen.class, remap = false)
public abstract class MixinWorldGenPredicate {

    @Redirect(method = "canGenerateInBlock(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;[Lcofh/cofhworld/util/random/WeightedBlock;)Z", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/block/state/pattern/BlockMatcher;forBlock(Lnet/minecraft/block/Block;)Lnet/minecraft/block/state/pattern/BlockMatcher;", remap = true), require = 1)
    private static BlockMatcher nova$replacementPredicate(final Block blockType, @Local(name = "genBlock") final WeightedBlock material) {
        return ((WeightedBlockPredicate) (Object) material).nova$replacementPredicate();
    }
}
