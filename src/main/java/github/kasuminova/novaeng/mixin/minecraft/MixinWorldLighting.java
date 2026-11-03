package github.kasuminova.novaeng.mixin.minecraft;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import github.kasuminova.novaeng.client.util.ClientLightingGuard;
import github.kasuminova.novaeng.client.util.LightingConstants;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(World.class)
public abstract class MixinWorldLighting {

    @Shadow
    @Final
    public boolean isRemote;

    @Redirect(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;checkLight(Lnet/minecraft/util/math/BlockPos;)Z"))
    private static boolean nova$skipLightCheck(final World world, final BlockPos pos) {
        return ClientLightingGuard.shouldProcessLighting(world) && world.checkLight(pos);
    }

    @Redirect(method = "markBlocksDirtyVertical",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;checkLightFor(Lnet/minecraft/world/EnumSkyBlock;Lnet/minecraft/util/math/BlockPos;)Z"))
    private static boolean nova$skipVerticalLightPropagation(final World world, final EnumSkyBlock lightType, final BlockPos pos) {
        return ClientLightingGuard.shouldProcessLighting(world) && world.checkLightFor(lightType, pos);
    }

    @ModifyReturnValue(method = "getCombinedLight(Lnet/minecraft/util/math/BlockPos;I)I", at = @At("RETURN"))
    private int nova$fullBrightCombinedLight(final int original) {
        return this.isRemote && ClientLightingGuard.isActive()
            ? LightingConstants.PACKED_FULL_BRIGHT
            : original;
    }
}
