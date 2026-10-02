package github.kasuminova.novaeng.mixin.codechickenlib;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.stellarcore.client.resource.ClasspathAssetIndex;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "codechicken.lib.model.loader.blockstate.CCBlockStateLoader", remap = false)
public abstract class MixinCCBlockStateLoader {

    /**
     * CodeChicken Lib walks every registered block and every blockstate location it maps to, and asks
     * whether its {@code <namespace>:cc_blockstates/<path>.json} exists by calling
     * {@code getResourceStack} and treating the thrown {@code FileNotFoundException} as "absent".
     * That stack lookup walks every resource pack of the namespace and builds a list, and the
     * exception has to be constructed each time; measured at 1676ms for one load.
     * The answer for jar-backed assets is already indexed by StellarCore's {@code ClasspathAssetIndex}
     * (prewarmed during model registry setup, before this event fires), so ask it first and fall back
     * to the original probe whenever the namespace is not covered by the index.
     */
    @Inject(method = "canLoad", at = @At("HEAD"), cancellable = true, remap = false)
    private static void nova$probeFromClasspathIndex(final IResourceManager resourceManager,
                                                     final ResourceLocation location,
                                                     final CallbackInfoReturnable<Boolean> cir) {
        if (!NovaEngCoreConfig.CLIENT.optimizeCodeChickenBlockstateScan) {
            return;
        }

        final ResourceLocation probe = new ResourceLocation(
                location.getNamespace(), "cc_blockstates/" + location.getPath() + ".json");
        if (probe.toString().endsWith("_factories.json")) {
            return;
        }

        final Boolean known = ClasspathAssetIndex.tryContains(probe);
        if (known != null) {
            cir.setReturnValue(known);
        }
    }
}
