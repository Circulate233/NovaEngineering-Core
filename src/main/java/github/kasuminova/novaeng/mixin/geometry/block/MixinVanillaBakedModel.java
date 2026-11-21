package github.kasuminova.novaeng.mixin.geometry.block;

import github.kasuminova.novaeng.client.geometry.CompactModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.property.IExtendedBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import java.util.List;

/**
 * Preserve Forge's original perspective, animation and item override wrapper.
 */
@Pseudo
@Mixin(targets = "net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper$1", remap = false)
public abstract class MixinVanillaBakedModel implements CompactModel {
    @Override
    public List<BakedQuad> novaeng$readQuads(final IBlockState state, final EnumFacing side, final long seed) {
        // Keep every original extended-state callback, including withProperty(null), in Forge's own method.
        if (state instanceof IExtendedBlockState) {
            return ((IBakedModel) this).getQuads(state, side, seed);
        }
        final IBakedModel parent = ((AccessorPerspectiveMapWrapper) this).novaeng$geometryParent();
        return parent instanceof CompactModel compact ? compact.novaeng$readQuads(state, side, seed) : parent.getQuads(state, side, seed);
    }

    @Override
    public boolean novaeng$isGeometryExposed() {
        final IBakedModel parent = ((AccessorPerspectiveMapWrapper) this).novaeng$geometryParent();
        return !(parent instanceof CompactModel compact) || compact.novaeng$isGeometryExposed();
    }
}
