package github.kasuminova.novaeng.mixin.journeymap;

import github.kasuminova.novaeng.common.performance.JourneyMapMetadataEpoch;
import net.minecraftforge.registries.ForgeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Common-side invalidation: no references to JourneyMap/client classes or eager prefix-cache loading. */
@Mixin(value = ForgeRegistry.class, remap = false)
public abstract class MixinForgeRegistryMetadataEpoch {
    @Inject(method = {"register", "registerAll", "sync", "clear", "addAlias", "addDummy", "loadIds", "processMissingEvent"},
        at = @At("HEAD"), require = 1)
    private void nova$beforeRegistryChange(final CallbackInfo ci) {
        JourneyMapMetadataEpoch.invalidate();
    }

    @Inject(method = {"register", "registerAll", "sync", "clear", "addAlias", "addDummy", "loadIds", "processMissingEvent"},
        at = @At("RETURN"), require = 1)
    private void nova$afterRegistryChange(final CallbackInfo ci) {
        JourneyMapMetadataEpoch.invalidate();
    }

    @Inject(method = {"add", "remove", "markDummy"}, at = @At("HEAD"), require = 1)
    private void nova$beforeRegistryValueChange(final CallbackInfoReturnable<?> cir) {
        JourneyMapMetadataEpoch.invalidate();
    }

    @Inject(method = {"add", "remove", "markDummy"}, at = @At("RETURN"), require = 1)
    private void nova$afterRegistryValueChange(final CallbackInfoReturnable<?> cir) {
        JourneyMapMetadataEpoch.invalidate();
    }
}
