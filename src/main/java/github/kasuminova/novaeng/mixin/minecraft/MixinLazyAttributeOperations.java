package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.AttributeModifierState;
import github.kasuminova.novaeng.common.performance.LazyModifierSet;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(ModifiableAttributeInstance.class)
public abstract class MixinLazyAttributeOperations implements AttributeModifierState {
    @Shadow
    @Final
    private Map<?, ?> mapByOperation;

    @Unique
    private Set<AttributeModifier> nova$addition;
    @Unique
    private Set<AttributeModifier> nova$multiplyBase;
    @Unique
    private Set<AttributeModifier> nova$multiplyTotal;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", remap = false,
        target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;", ordinal = 0), require = 1)
    private HashMap<?, ?> nova$omitOperationMap() {
        return null;
    }

    // The constructor's empty-set factory and following put are replaced together.
    // All operation-map reads below use the three slots; public collections remain live and mutable.
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
        if (map != mapByOperation) return map.get(key);
        if (!(key instanceof Integer operation)) return null;
        return switch (operation) {
            case 0 -> nova$addition == null ? nova$addition = new LazyModifierSet() : nova$addition;
            case 1 -> nova$multiplyBase == null ? nova$multiplyBase = new LazyModifierSet() : nova$multiplyBase;
            case 2 -> nova$multiplyTotal == null ? nova$multiplyTotal = new LazyModifierSet() : nova$multiplyTotal;
            default -> null;
        };
    }

    @Override
    public boolean nova$hasModifiers() {
        return nova$addition != null && !nova$addition.isEmpty()
            || nova$multiplyBase != null && !nova$multiplyBase.isEmpty()
            || nova$multiplyTotal != null && !nova$multiplyTotal.isEmpty();
    }
}
