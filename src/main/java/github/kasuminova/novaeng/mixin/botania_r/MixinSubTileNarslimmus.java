package github.kasuminova.novaeng.mixin.botania_r;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vazkii.botania.common.block.subtile.generating.SubTileNarslimmus;

@Mixin(value = SubTileNarslimmus.class, remap = false)
public class MixinSubTileNarslimmus {

    @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Ljava/lang/Math;pow(DD)D"))
    public double onUpdateI(double a, double b) {
        return Math.pow(a, b) * 1.5;
    }
}
