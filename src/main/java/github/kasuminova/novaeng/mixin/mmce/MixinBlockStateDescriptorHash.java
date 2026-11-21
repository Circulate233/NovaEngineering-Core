package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.performance.BlockStateDescriptorHash;
import github.kasuminova.novaeng.common.performance.mmce.DescriptorCanonicalPool;
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = IBlockStateDescriptor.class, remap = false)
public abstract class MixinBlockStateDescriptorHash {

    @Unique
    private static final DescriptorCanonicalPool<IBlockStateDescriptor> nova$descriptors = new DescriptorCanonicalPool<>();

    /**
     * @author NovaEngineering
     * @reason Retain the upstream reset boundary across every descriptor stripe.
     */
    @Overwrite
    public static void clearPool() {
        nova$descriptors.clear();
    }

    /**
     * @author NovaEngineering
     * @reason Parallel machine parsing and rotation need not serialize unrelated descriptor hashes.
     */
    @Overwrite
    public IBlockStateDescriptor canonicalize() {
        return nova$descriptors.canonicalize((IBlockStateDescriptor) (Object) this);
    }

    @Redirect(method = "hashCode", at = @At(value = "INVOKE", target = "Ljava/util/List;hashCode()I"), require = 1)
    private int nova$disperseSingleStateHashes(final List<IBlockState> states) {
        return BlockStateDescriptorHash.hash(states);
    }
}
