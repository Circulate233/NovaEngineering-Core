package github.kasuminova.novaeng.mixin.botania_r;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.common.block.subtile.generating.SubTileDandelifeon;

@Mixin(value = SubTileDandelifeon.class, remap = false)
public class MixinSubTileDandelifeon {

    @Redirect(method = "setBlockForGeneration", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    public int setBlockForGeneration(int a, int b) {
        return Math.min(200, b) * 2;
    }

    @Inject(method = "getMaxMana", at = @At("HEAD"), cancellable = true)
    public void getMaxMana(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(500000);
    }
}
