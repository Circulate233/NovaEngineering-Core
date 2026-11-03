package github.kasuminova.novaeng.common.performance;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public final class AstralAttributeOwner {
    private static final VarHandle OWNER = findOwner();

    private AstralAttributeOwner() {
    }

    private static VarHandle findOwner() {
        try {
            return MethodHandles.privateLookupIn(AbstractAttributeMap.class, MethodHandles.lookup())
                                .findVarHandle(AbstractAttributeMap.class, "as_entity", EntityLivingBase.class);
        } catch (final ReflectiveOperationException absent) {
            return null;
        }
    }

    public static AbstractAttributeMap bind(final AbstractAttributeMap map, final EntityLivingBase entity) {
        if (OWNER != null && map != null && OWNER.get(map) != entity) OWNER.set(map, entity);
        return map;
    }

    public static EntityLivingBase get(final AbstractAttributeMap map) {
        return OWNER == null || map == null ? null : (EntityLivingBase) OWNER.get(map);
    }
}
