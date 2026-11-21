package github.kasuminova.novaeng.mixin.geometry.block;

import github.kasuminova.novaeng.client.geometry.CompactModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

/**
 * VintageFix replaces Forge's anonymous vanilla wrapper with this exact delegating wrapper.
 */
@Mixin(value = PerspectiveMapWrapper.class, remap = false)
public abstract class MixinPerspectiveMapWrapper implements CompactModel {
    @Override
    public List<BakedQuad> novaeng$readQuads(final IBlockState state, final EnumFacing side, final long seed) {
        // Subclasses may override getQuads with animation or other callbacks. Their own
        // approved adapters can override this method; every other subclass stays public.
        if (((Object) this).getClass() != PerspectiveMapWrapper.class)
            return ((IBakedModel) this).getQuads(state, side, seed);
        final IBakedModel parent = ((AccessorPerspectiveMapWrapper) this).novaeng$geometryParent();
        return parent instanceof CompactModel compact ? compact.novaeng$readQuads(state, side, seed) : parent.getQuads(state, side, seed);
    }

    @Override
    public boolean novaeng$isGeometryExposed() {
        if (((Object) this).getClass() != PerspectiveMapWrapper.class) return true;
        final IBakedModel parent = ((AccessorPerspectiveMapWrapper) this).novaeng$geometryParent();
        return !(parent instanceof CompactModel compact) || compact.novaeng$isGeometryExposed();
    }
}
