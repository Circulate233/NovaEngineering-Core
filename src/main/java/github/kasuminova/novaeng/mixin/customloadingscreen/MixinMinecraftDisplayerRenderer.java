package github.kasuminova.novaeng.mixin.customloadingscreen;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "alexiil.mc.mod.load.render.MinecraftDisplayerRenderer", remap = false)
public abstract class MixinMinecraftDisplayerRenderer {

    /**
     * {@code ClsManager.load()} refreshes the resource manager immediately before this constructor
     * runs, and nothing between the two calls touches the resource pack list, so the second refresh
     * redoes the same work. Keep the first refresh's resource manager and the constructor's
     * subsequent local texture-manager and font reloads.
     */
    @Redirect(method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;refreshResources()V", remap = true),
        remap = false, require = 0, expect = 0)
    private void nova$skipRedundantResourceReload(final Minecraft minecraft) {
    }
}
