package github.kasuminova.novaeng.mixin.customloadingscreen;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "alexiil.mc.mod.load.render.MinecraftDisplayerRenderer", remap = false)
public abstract class MixinMinecraftDisplayerRenderer {

    /**
     * {@code ClsManager.load()} refreshes the resource manager immediately before this constructor
     * runs, and nothing between the two calls touches the resource pack list, so the second refresh
     * redoes the same work (654ms measured). Dropping it keeps the resource manager produced by the
     * first refresh, which is what the constructor hands to its texture manager right afterwards.
     */
    @Redirect(method = "<init>",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;refreshResources()V"),
              remap = false)
    private void nova$skipRedundantResourceReload(final Minecraft minecraft) {
        if (!NovaEngCoreConfig.CLIENT.optimizeCustomLoadingScreen) {
            minecraft.refreshResources();
        }
    }
}
