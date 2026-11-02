package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.common.performance.ForgelinObjectAdapter;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LoadController.class, remap = false)
public abstract class MixinForgelinConstructionAdapter {
    @Inject(method = "sendEventToModContainer", at = @At(value = "INVOKE",
        target = "Lcom/google/common/eventbus/EventBus;post(Ljava/lang/Object;)V"), require = 1)
    private void nova$installObjectAdapter(final FMLEvent stateEvent, final ModContainer mc, final CallbackInfo ci) {
        if (stateEvent instanceof FMLConstructionEvent) {
            ForgelinObjectAdapter.install(mc);
        }
    }
}
