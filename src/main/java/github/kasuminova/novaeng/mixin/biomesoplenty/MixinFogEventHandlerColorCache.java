package github.kasuminova.novaeng.mixin.biomesoplenty;

import biomesoplenty.common.handler.FogEventHandler;
import github.kasuminova.novaeng.client.util.ClientWorldMemos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FogEventHandler.class, remap = false)
public abstract class MixinFogEventHandlerColorCache {

    @Shadow
    private static double fogX;

    @Shadow
    private static double fogZ;

    @Shadow
    private static boolean fogInit;

    @Inject(method = "onRenderFog", at = @At("HEAD"), require = 1)
    private void nova$invalidateOnWorldChange(final EntityViewRenderEvent.RenderFogEvent event, final CallbackInfo ci) {
        final World world = event.getEntity().world;
        if (ClientWorldMemos.FOG.select(world)) {
            fogInit = false;
        }
    }

    @Redirect(
        method = "onRenderFog",
        at = @At(value = "FIELD", target = "Lbiomesoplenty/common/handler/FogEventHandler;fogX:D", opcode = Opcodes.PUTSTATIC),
        require = 1
    )
    private static void nova$cacheBlockX(final double value) {
        fogX = Math.floor(value);
    }

    @Redirect(
        method = "onRenderFog",
        at = @At(value = "FIELD", target = "Lbiomesoplenty/common/handler/FogEventHandler;fogZ:D", opcode = Opcodes.PUTSTATIC),
        require = 1
    )
    private static void nova$cacheBlockZ(final double value) {
        fogZ = Math.floor(value);
    }
}
