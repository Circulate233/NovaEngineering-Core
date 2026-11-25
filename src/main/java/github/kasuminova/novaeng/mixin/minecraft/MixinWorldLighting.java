package github.kasuminova.novaeng.mixin.minecraft;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import github.kasuminova.novaeng.client.util.ClientLightingGuard;
import github.kasuminova.novaeng.client.util.LightingConstants;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(World.class)
public abstract class MixinWorldLighting {

    @Shadow
    @Final
    public boolean isRemote;

    @ModifyReturnValue(method = "getCombinedLight(Lnet/minecraft/util/math/BlockPos;I)I", at = @At("RETURN"))
    private int nova$fullBrightCombinedLight(final int original) {
        return this.isRemote && ClientLightingGuard.isActive()
            ? LightingConstants.PACKED_FULL_BRIGHT
            : original;
    }
}
