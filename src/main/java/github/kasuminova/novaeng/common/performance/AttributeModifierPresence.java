package github.kasuminova.novaeng.common.performance;

import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;

public final class AttributeModifierPresence {
    private AttributeModifierPresence() {
    }

    public static boolean isEmpty(final IAttributeInstance attribute) {
        if (attribute.getClass() != ModifiableAttributeInstance.class) return attribute.getModifiers().isEmpty();
        if (attribute instanceof AttributeModifierState state) return !state.nova$hasModifiers();
        for (int operation = 0; operation < 3; operation++) {
            if (!attribute.getModifiersByOperation(operation).isEmpty()) return false;
        }
        return true;
    }
}
