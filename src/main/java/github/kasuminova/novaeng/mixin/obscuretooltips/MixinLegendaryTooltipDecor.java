package github.kasuminova.novaeng.mixin.obscuretooltips;

import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig;
import com.anthonyhilyard.legendarytooltips.render.TooltipDecor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TooltipDecor.class, remap = false)
public class MixinLegendaryTooltipDecor {

    @Redirect(method = "drawBorder", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
        target = "Lcom/anthonyhilyard/legendarytooltips/LegendaryTooltipsConfig;nameSeparator:Z"))
    private static boolean novaeng$noDuplicateSeparator(final LegendaryTooltipsConfig config) {
        return false;
    }
}
