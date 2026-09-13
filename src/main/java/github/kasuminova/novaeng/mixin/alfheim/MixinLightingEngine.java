package github.kasuminova.novaeng.mixin.alfheim;

import github.kasuminova.novaeng.client.util.ClientLightingGuard;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.redstudio.alfheim.lighting.LightingEngine", remap = false)
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
