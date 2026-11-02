package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.AttributeModifierState;
import github.kasuminova.novaeng.common.performance.LazyModifierSet;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(ModifiableAttributeInstance.class)
public abstract class MixinLazyAttributeOperations implements AttributeModifierState {
    @Shadow
    @Final
    private Map<Integer, Set<AttributeModifier>> mapByOperation;

    // The constructor's empty-set factory and following put are replaced together.
    // The private map remains non-null; no null set is published through any attribute API.
    @Redirect(method = "<init>", at = @At(value = "INVOKE", remap = false,
        target = "Lcom/google/common/collect/Sets;newHashSet()Ljava/util/HashSet;"), require = 1)
    private HashSet<AttributeModifier> nova$deferEmptySet() {
        return null;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", remap = false,
        target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), require = 1)
    private Object nova$deferOperation(final Map<Integer, Set<AttributeModifier>> map, final Object key, final Object value) {
        if (map == mapByOperation && value == null) return null;
        return ((Map<Object, Object>) (Map<?, ?>) map).put(key, value);
    }

    @Redirect(method = {"getModifiersByOperation", "applyModifier", "removeModifier(Lnet/minecraft/entity/ai/attributes/AttributeModifier;)V"},
        at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"), require = 4)
    private Object nova$liveOperation(final Map<?, ?> map, final Object key) {
        final Object current = map.get(key);
        if (map != mapByOperation || current != null || !(key instanceof Integer operation) || operation < 0 || operation > 2)
            return current;
        final Set<AttributeModifier> modifiers = new LazyModifierSet();
        mapByOperation.put(operation, modifiers);
        return modifiers;
    }

    @Override
    public boolean nova$hasModifiers() {
        for (int operation = 0; operation < 3; operation++) {
            final Set<AttributeModifier> modifiers = mapByOperation.get(operation);
            if (modifiers != null && !modifiers.isEmpty()) return true;
        }
        return false;
    }
}
