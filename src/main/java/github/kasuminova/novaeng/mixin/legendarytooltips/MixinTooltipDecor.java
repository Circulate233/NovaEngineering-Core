package github.kasuminova.novaeng.mixin.legendarytooltips;

import com.anthonyhilyard.legendarytooltips.render.TooltipDecor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Map;

@Mixin(value = TooltipDecor.class, remap = false)
public class MixinTooltipDecor {

    @Redirect(method = "drawBorder", at = @At(value = "INVOKE", target = "Ljava/util/Map;containsKey(Ljava/lang/Object;)Z"))
    private static boolean containsKeyMixin(Map<Object, Object> instance, Object o) {
        boolean hasKey = instance.containsKey(o);
        if (hasKey) {
            if (instance.get(o) instanceof List<?> list) {
                if (list.isEmpty()) {
                    return false;
                }
            }
        }
        return hasKey;
    }
}
