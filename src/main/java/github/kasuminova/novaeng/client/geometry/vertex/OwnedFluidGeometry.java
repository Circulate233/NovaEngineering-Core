package github.kasuminova.novaeng.client.geometry.vertex;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Base mesh access, separate from the cache subclass's state-dependent mesh selection.
 */
public interface OwnedFluidGeometry {
    List<BakedQuad> novaeng$readBaseQuads(@Nullable EnumFacing side);
}
