package github.kasuminova.novaeng.mixin.minecraft;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import net.minecraft.util.ClassInheritanceMultiMap;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keep the existing per-section index, but do not prepopulate every historical query class.
 */
@Mixin(value = ClassInheritanceMultiMap.class, priority = 500)
public abstract class MixinClassInheritanceMultiMapLazy<T> {
    @Shadow
    @Final
    @Mutable
    private Map<Class<?>, List<T>> map;
    @Shadow
    @Final
    @Mutable
    private Set<Class<?>> knownKeys;
    @Shadow
    @Final
    private Class<T> baseClass;
    @Shadow
    @Final
    private List<T> values;

    @Redirect(method = "<init>", at = @At(value = "FIELD",
        target = "Lnet/minecraft/util/ClassInheritanceMultiMap;ALL_KNOWN:Ljava/util/Set;",
        opcode = Opcodes.GETSTATIC), require = 1)
    private Set<Class<?>> nova$skipUnrequestedClasses() {
        return ReferenceSets.emptySet();
    }

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void nova$compactEmptyIndex(final Class<T> baseClassIn, final CallbackInfo ci) {
        // Runs after Stellar's constructor replacement. Only the base mapping exists at this point.
        final Map<Class<?>, List<T>> compact = new Reference2ObjectOpenHashMap<>(2);
        compact.putAll(map);
        map = compact;
        final Set<Class<?>> requested = new ReferenceOpenHashSet<>(2);
        requested.addAll(knownKeys);
        knownKeys = requested;
    }
}
