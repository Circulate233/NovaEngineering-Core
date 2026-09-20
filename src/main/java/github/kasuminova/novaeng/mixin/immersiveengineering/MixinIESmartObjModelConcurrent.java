package github.kasuminova.novaeng.mixin.immersiveengineering;

import blusunrize.immersiveengineering.client.models.IESmartObjModel;
import blusunrize.immersiveengineering.client.models.smart.ConnModelReal;
import github.kasuminova.novaeng.client.model.async.NonBlockingHashMapFacade;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;

/** Replaces IE smart OBJ's shared bake map with a lock-free map. */
@Mixin(value = IESmartObjModel.class, remap = false)
public abstract class MixinIESmartObjModelConcurrent {

    @Shadow
    @Mutable
    public static HashMap<ConnModelReal.ExtBlockstateAdapter, List<BakedQuad>> modelCache;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false, require = 1)
    private static void nova$useConcurrentCache(final CallbackInfo ci) {
        modelCache = new NonBlockingHashMapFacade<>();
    }
}
