package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.performance.BlockStateDescriptorHash;
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = IBlockStateDescriptor.class, remap = false)
public abstract class MixinBlockStateDescriptorHash {

    @Redirect(method = "hashCode", at = @At(value = "INVOKE", target = "Ljava/util/List;hashCode()I"), require = 1)
    private int nova$disperseSingleStateHashes(final List<IBlockState> states) {
        return BlockStateDescriptorHash.hash(states);
    }
}
