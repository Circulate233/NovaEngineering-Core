package github.kasuminova.novaeng.mixin.botania_r;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vazkii.botania.api.subtile.SubTileGenerating;

@Mixin(value = SubTileGenerating.class, remap = false)
public class MixinSubTileGenerating {

    @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Lvazkii/botania/api/subtile/SubTileGenerating;isPassiveFlower()Z"))
    public boolean isPassiveFlowerR(SubTileGenerating instance) {
        return false;
    }
}
