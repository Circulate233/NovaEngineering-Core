package github.kasuminova.novaeng.mixin.theoneprobe;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.kasuminova.novaeng.client.performance.BoundedParsedStrings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "mcjty.theoneprobe.config.Config", remap = false)
public abstract class MixinConfigTextStyleCache {
    @Unique
    private static final BoundedParsedStrings<String> nova$textStyles = new BoundedParsedStrings<>(128);

    @WrapMethod(method = "configToTextFormat")
    private static String nova$cachedTextStyle(final String input, final Operation<String> original) {
        final BoundedParsedStrings.Entry<String> cached = nova$textStyles.get(input);
        if (cached != null) return cached.value();
        // The current input is the config value itself: changed/synced config never uses an old key.
        final String parsed = original.call(input);
        nova$textStyles.put(input, parsed);
        return parsed;
    }
}
