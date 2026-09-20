package github.kasuminova.novaeng.mixin.alfheim;

import github.kasuminova.novaeng.client.util.ClientLightingGuard;
import dev.redstudio.alfheim.lighting.LightingEngine;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LightingEngine.class, remap = false)
public abstract class MixinLightingEngine {

    @Shadow
    @Final
    private World world;

    @Inject(method = "scheduleLightUpdate(Lnet/minecraft/world/EnumSkyBlock;Lnet/minecraft/util/math/BlockPos;)V",
        at = @At("HEAD"), cancellable = true)
    private void nova$skipScheduleLightUpdate(final CallbackInfo ci) {
        if (this.world.isRemote && ClientLightingGuard.isActive()) {
            ci.cancel();
        }
    }
}
