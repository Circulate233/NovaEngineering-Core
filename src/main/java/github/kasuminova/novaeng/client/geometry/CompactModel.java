package github.kasuminova.novaeng.client.geometry;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Only for synchronous built-in consumers. Public IBakedModel readers receive stable writable quads.
 */
public interface CompactModel {
    List<BakedQuad> novaeng$readQuads(@Nullable IBlockState state, @Nullable EnumFacing face, long seed);

    boolean novaeng$isGeometryExposed();
}
