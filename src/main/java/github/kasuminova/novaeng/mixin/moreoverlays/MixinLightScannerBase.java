package github.kasuminova.novaeng.mixin.moreoverlays;

import at.feldim2425.moreoverlays.api.lightoverlay.LightScannerBase;
import github.kasuminova.novaeng.client.moreoverlays.LightScanCache;
import github.kasuminova.novaeng.client.moreoverlays.PrimitiveLightScanner;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = LightScannerBase.class, remap = false)
public abstract class MixinLightScannerBase implements PrimitiveLightScanner {
    @Shadow
    protected List<Pair<BlockPos, Byte>> overlayCache;
    @Unique
    private LightScanCache novaeng$lightCache;

    @Override
    public LightScanCache novaeng$primitiveLightModes() {
        if (!LightScanCache.supports((LightScannerBase) (Object) this)) return null;
        if (novaeng$lightCache == null) novaeng$lightCache = new LightScanCache();
        return novaeng$lightCache;
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void novaeng$scan(final EntityPlayer player, final CallbackInfo ci) {
        final LightScanCache cache = novaeng$primitiveLightModes();
        if (cache == null) return;
        cache.update((LightScannerBase) (Object) this, player, overlayCache);
        ci.cancel();
    }

    @Inject(method = "getLightModes", at = @At("HEAD"), cancellable = true)
    private void novaeng$expose(final CallbackInfoReturnable<List<Pair<BlockPos, Byte>>> cir) {
        final LightScanCache cache = novaeng$primitiveLightModes();
        if (cache != null) cir.setReturnValue(cache.expose(overlayCache));
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void novaeng$clear(final CallbackInfo ci) {
        if (novaeng$lightCache != null) novaeng$lightCache.clear();
    }
}
