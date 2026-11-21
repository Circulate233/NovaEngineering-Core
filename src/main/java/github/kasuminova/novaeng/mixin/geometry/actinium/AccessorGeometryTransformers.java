package github.kasuminova.novaeng.mixin.geometry.actinium;

import com.dhj.actinium.api.render.terrain.BlockQuadTransformer;
import com.dhj.actinium.api.render.terrain.BlockQuadTransformerHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(value = BlockQuadTransformerHolder.class, remap = false)
public interface AccessorGeometryTransformers {
    // Actinium owns this field and requires copy-on-write iteration and the exact descriptor.
    @Accessor("TRANSFORMERS")
    static CopyOnWriteArrayList<BlockQuadTransformer> novaeng$transformers() {
        throw new AssertionError();
    }
}
