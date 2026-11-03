package github.kasuminova.novaeng.mixin.ic2;

import ic2.core.uu.LeanItemStack;
import ic2.core.uu.UuGraph;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(value = UuGraph.Node.class, remap = false)
public class MixinUuGraph$Node {

    @Shadow
    public Set<UuGraph.NodeTransform> provides;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void onInit(LeanItemStack stack, CallbackInfo ci) {
        provides = new ObjectOpenHashSet<>();
    }
}
