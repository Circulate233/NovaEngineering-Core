package github.kasuminova.novaeng.mixin.mekanism;

import com.google.common.base.Predicate;
import github.kasuminova.novaeng.common.performance.MekanismWorldgenFactories;
import mekanism.common.world.GenHandler;
import mekanism.common.world.WorldGenSalt;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.world.gen.feature.WorldGenMinable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GenHandler.class, remap = false)
public abstract class MixinWorldgenFactories {
    @Redirect(method = "generate", at = @At(value = "INVOKE", remap = true,
        target = "Lnet/minecraft/block/state/pattern/BlockMatcher;forBlock(Lnet/minecraft/block/Block;)Lnet/minecraft/block/state/pattern/BlockMatcher;"), require = 6)
    private BlockMatcher nova$matcher(final Block block) {
        return MekanismWorldgenFactories.matcher(block);
    }

    @Redirect(method = "generate", at = @At(value = "NEW", target = "net/minecraft/world/gen/feature/WorldGenMinable", remap = true), require = 6)
    private WorldGenMinable nova$ore(final IBlockState state, final int size, final Predicate<IBlockState> predicate) {
        return MekanismWorldgenFactories.ore(state, size, predicate);
    }

    @Redirect(method = "generate", at = @At(value = "NEW", target = "mekanism/common/world/WorldGenSalt"), require = 1)
    private WorldGenSalt nova$salt(final int size) {
        return MekanismWorldgenFactories.salt(size);
    }
}
