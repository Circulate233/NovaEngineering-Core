package github.kasuminova.novaeng.mixin.geometry.block;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PerspectiveMapWrapper.class, remap = false)
public interface AccessorPerspectiveMapWrapper {
    @Accessor("parent")
    IBakedModel novaeng$geometryParent();
}
