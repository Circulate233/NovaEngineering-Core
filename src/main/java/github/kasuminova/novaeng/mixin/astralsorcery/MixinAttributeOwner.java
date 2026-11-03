package github.kasuminova.novaeng.mixin.astralsorcery;

import github.kasuminova.novaeng.common.performance.AstralAttributeOwner;
import hellfirepvp.astralsorcery.common.event.AttributeEvent;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = AttributeEvent.class, remap = false)
public abstract class MixinAttributeOwner {
    /**
     * @author NovaEngineering
     * @reason Bind the ASM-provided owner through a cached VarHandle without reflective Field.set calls.
     */
    @Overwrite
    public static AbstractAttributeMap markToPlayer(final AbstractAttributeMap map, final EntityLivingBase entity) {
        return AstralAttributeOwner.bind(map, entity);
    }

    /**
     * @author NovaEngineering
     * @reason Read the same ASM-provided field and retain the absent-field fallback.
     */
    @Overwrite
    private static EntityLivingBase getEntity(final AbstractAttributeMap map) {
        return AstralAttributeOwner.get(map);
    }
}
