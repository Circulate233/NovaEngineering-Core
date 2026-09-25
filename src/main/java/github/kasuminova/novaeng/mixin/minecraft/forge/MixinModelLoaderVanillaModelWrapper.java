package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Collection;
import java.util.Map;

/**
 * Rewrites ModelLoader.VanillaModelWrapper#getDependencies so the dependency set is built with an
 * ImmutableSet.Builder directly instead of filling a mutable HashSet and copying it afterwards.
 */
@Mixin(targets = "net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper", remap = false)
public abstract class MixinModelLoaderVanillaModelWrapper {

    @Shadow
    @Final
    ModelLoader this$0;

    @Shadow
    @Final
    private ResourceLocation location;

    @Shadow
    @Final
    private ModelBlock model;

    /**
     * @author circulation
     * @reason 覆写依赖收集过程，直接使用 ImmutableSet.Builder 构建依赖集合，避免先创建可变 HashSet 再拷贝一次
     */
    @Overwrite
    public Collection<ResourceLocation> getDependencies() {
        ImmutableSet.Builder<ResourceLocation> builder = ImmutableSet.builder();
        Map<ModelResourceLocation, IModel> stateModels = ((AccessorModelLoader) (Object) this.this$0).getStateModels();
        for (ResourceLocation dep : model.getOverrideLocations()) {
            if (!location.equals(dep)) {
                builder.add(dep);
                stateModels.put(ModelLoader.getInventoryVariant(dep.toString()),
                    ModelLoaderRegistry.getModelOrLogError(dep, "Could not load override model " + dep + " for model " + location));
            }
        }
        if (model.getParentLocation() != null && !model.getParentLocation().getPath().startsWith("builtin/")) {
            builder.add(model.getParentLocation());
        }
        return builder.build();
    }

    @Mixin(value = ModelLoader.class, remap = false)
    public interface AccessorModelLoader {

        @Accessor("stateModels")
        Map<ModelResourceLocation, IModel> getStateModels();
    }
}
