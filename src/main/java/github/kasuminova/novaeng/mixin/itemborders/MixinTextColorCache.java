package github.kasuminova.novaeng.mixin.itemborders;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.kasuminova.novaeng.client.performance.BoundedParsedStrings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "com.anthonyhilyard.itemborders.util.TextColor", remap = false)
public abstract class MixinTextColorCache {
    @Unique
    private static final BoundedParsedStrings<Integer> nova$parsedColors = new BoundedParsedStrings<>(128);

    @WrapMethod(method = "parseColor")
    private static Integer nova$cachedColor(final String input, final Operation<Integer> original) {
        final BoundedParsedStrings.Entry<Integer> cached = nova$parsedColors.get(input);
        if (cached != null) return cached.value();
        // Invalid colors (null) are cached too; exceptions are left to the original parser.
        final Integer parsed = original.call(input);
        nova$parsedColors.put(input, parsed);
        return parsed;
    }
}
