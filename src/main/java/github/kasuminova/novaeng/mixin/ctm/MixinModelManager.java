package github.kasuminova.novaeng.mixin.ctm;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.kasuminova.novaeng.client.ctm.CtmBakeTraceIndex;
import github.kasuminova.novaeng.client.ctm.CtmBakeTraceIndexImpl;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;

/** Bounds CTM bake traces to one complete model reload, including exceptional exits. */
@Mixin(ModelManager.class)
public abstract class MixinModelManager {

    @WrapMethod(method = "onResourceManagerReload", require = 1)
    private void nova$withCtmBakeGeneration(final IResourceManager resourceManager,
                                            final Operation<Void> original) {
        final CtmBakeTraceIndex traces = CtmBakeTraceIndexImpl.instance();
        traces.begin();
        try {
            original.call(resourceManager);
        } finally {
            traces.end();
        }
    }
}
