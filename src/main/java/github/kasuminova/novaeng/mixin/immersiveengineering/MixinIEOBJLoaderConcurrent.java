package github.kasuminova.novaeng.mixin.immersiveengineering;

import blusunrize.immersiveengineering.client.models.obj.IEOBJLoader;
import blusunrize.immersiveengineering.client.models.obj.IEOBJModel;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/** Makes IE's OBJ loader caches safe for StellarCore's parallel model loader. */
@Mixin(value = IEOBJLoader.class, remap = false)
public abstract class MixinIEOBJLoaderConcurrent {

    @Shadow
    @Final
    @Mutable
    private Map<ResourceLocation, IEOBJModel> cache;

    @Shadow
    @Final
    @Mutable
    private Map<ResourceLocation, Exception> errors;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 1)
    private void nova$useConcurrentCaches(final CallbackInfo ci) {
        this.cache = new NonBlockingHashMap<>();
        this.errors = new NonBlockingHashMap<>();
    }
}
