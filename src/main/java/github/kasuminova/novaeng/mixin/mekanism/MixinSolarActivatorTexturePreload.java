package github.kasuminova.novaeng.mixin.mekanism;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import mekanism.multiblockmachine.client.MultiblockMachineClientProxy;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MultiblockMachineClientProxy.class, remap = false)
public abstract class MixinSolarActivatorTexturePreload {
    @WrapWithCondition(method = "registerTESRs", at = @At(value = "INVOKE",
        target = "Lmekanism/client/render/MekanismRenderer;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"), require = 0, expect = 0)
    private boolean nova$keepOtherPreloads(final ResourceLocation texture) {
        // Earlier CE releases have no screen preload in registerTESRs; leave those versions untouched.
        // Both model passes bind SCREEN before drawing, including when rendered as an item.
        return !("mekanismmultiblockmachine".equals(texture.getNamespace())
            && "render/machine/largesolarneutronactivator/screen.png".equals(texture.getPath()));
    }
}
