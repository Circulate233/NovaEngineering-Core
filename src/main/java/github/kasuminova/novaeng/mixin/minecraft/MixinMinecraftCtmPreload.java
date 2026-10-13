package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.chisel.ctm.api.util.RenderContextList;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftCtmPreload {

    @Inject(method = "init()V", at = @At("HEAD"), require = 1)
    private void nova$preloadCtmContextList(final CallbackInfo callbackInfo) {
        RenderContextList.class.getName();
    }
}
