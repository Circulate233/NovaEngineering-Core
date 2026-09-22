package github.kasuminova.novaeng.mixin.zenscript;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import stanhebben.zenscript.compiler.TypeRegistry;
import stanhebben.zenscript.type.ZenType;

import java.util.Map;

/**
 * Swaps the compiler's type table for a fastutil map.
 *
 * <p>Every type the compiler resolves is looked up in this table, so its lookups are on the hot path of script
 * compilation. The map is replaced after the constructor has populated the primitives, keeping every preloaded
 * entry and the {@code Map} interface the rest of the compiler sees.</p>
 */
@Mixin(value = TypeRegistry.class, remap = false)
public class MixinTypeRegistry {

    @Shadow
    @Final
    @Mutable
    private Map<Class<?>, ZenType> types;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 1)
    private void nova$useFastutilTypes(final CallbackInfo ci) {
        this.types = new Reference2ObjectOpenHashMap<>(this.types);
    }

}
