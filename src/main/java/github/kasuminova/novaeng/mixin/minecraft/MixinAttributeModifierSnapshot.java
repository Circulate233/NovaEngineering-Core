package github.kasuminova.novaeng.mixin.minecraft;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Mixin(ModifiableAttributeInstance.class)
public abstract class MixinAttributeModifierSnapshot {

    @Shadow
    @Final
    private Map<UUID, AttributeModifier> mapByUUID;

    @Shadow
    public abstract Collection<AttributeModifier> getModifiersByOperation(int operation);

    /**
     * @author circulation
     * @reason Keep an independent mutable modifier snapshot without allocating one hash node per modifier.
     */
    @Overwrite
    public Collection<AttributeModifier> getModifiers() {
        ObjectOpenHashSet<AttributeModifier> result = null;
        for (int operation = 0; operation < 3; operation++) {
            final Collection<AttributeModifier> modifiers = this.getModifiersByOperation(operation);
            if (result == null) {
                if (modifiers.isEmpty()) {
                    continue;
                }
                result = new ObjectOpenHashSet<>(Math.max(4, this.mapByUUID.size()));
            }
            result.addAll(modifiers);
        }
        return result == null ? new ObjectArraySet<>() : result;
    }
}
