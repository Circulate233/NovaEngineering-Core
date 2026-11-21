package github.kasuminova.novaeng.mixin.geometry.vertex;

import com.google.common.cache.LoadingCache;
import github.kasuminova.novaeng.client.geometry.CompactModel;
import github.kasuminova.novaeng.client.geometry.vertex.OwnedFluidGeometry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.property.IExtendedBlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * The trusted reader retains Forge's quantization, property callbacks and bounded model cache.
 */
@Mixin(targets = "net.minecraftforge.client.model.ModelFluid$CachingBakedFluid", remap = false)
public abstract class MixinCachingBakedFluidRead implements CompactModel {
    @Shadow
    @Final
    private LoadingCache<Long, ?> modelCache;

    @Shadow
    private static int[] getCorners(final Optional<IExtendedBlockState> stateOption) {
        throw new AssertionError();
    }

    @Shadow
    private static int getFlow(final Optional<IExtendedBlockState> stateOption) {
        throw new AssertionError();
    }

    @Shadow
    private static boolean[] getOverlay(final Optional<IExtendedBlockState> stateOption) {
        throw new AssertionError();
    }

    @Override
    public List<BakedQuad> novaeng$readQuads(@Nullable final IBlockState state, @Nullable final EnumFacing side,
                                             final long rand) {
        if (side != null && state instanceof IExtendedBlockState extended) {
            final Optional<IExtendedBlockState> stateOption = Optional.of(extended);
            final int[] corners = getCorners(stateOption);
            final int flow = getFlow(stateOption);
            final boolean[] overlays = getOverlay(stateOption);
            long key = 0;
            for (int i = 3; i >= 0; i--) {
                key <<= 1;
                key |= overlays[i] ? 1 : 0;
            }
            key <<= 11;
            key |= flow + 1024;
            for (int i = 3; i >= 0; i--) {
                key <<= 10;
                key |= corners[i];
            }
            key <<= 1;
            key |= 1;
            return ((CompactModel) modelCache.getUnchecked(key)).novaeng$readQuads(state, side, rand);
        }
        // Invoke the base trusted reader without reentering this override.
        return ((OwnedFluidGeometry) this).novaeng$readBaseQuads(side);
    }
}
