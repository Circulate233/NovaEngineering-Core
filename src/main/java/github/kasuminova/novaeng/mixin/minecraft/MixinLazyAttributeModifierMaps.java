package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.SharedEmptyHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mixin(ModifiableAttributeInstance.class)
public abstract class MixinLazyAttributeModifierMaps {
    @Shadow
    @Final
    @Mutable
    private Map<String, Set<AttributeModifier>> mapByName;

    @Shadow
    @Final
    @Mutable
    private Map<UUID, AttributeModifier> mapByUUID;

    // Ordinal zero belongs to MixinLazyAttributeOperations. These two maps are
    // private indices, never the mutable operation collections returned to callers.
    @Redirect(method = "<init>", at = @At(value = "INVOKE", remap = false,
        target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;", ordinal = 1), require = 1)
    private HashMap<String, Set<AttributeModifier>> nova$emptyNameIndex() {
        return SharedEmptyHashMap.instance();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", remap = false,
        target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;", ordinal = 2), require = 1)
    private HashMap<UUID, AttributeModifier> nova$emptyUuidIndex() {
        return SharedEmptyHashMap.instance();
    }

    @Inject(method = "applyModifier", at = @At("HEAD"), require = 1)
    private void nova$allocateModifierIndices(final AttributeModifier modifier, final CallbackInfo ci) {
        if (this.mapByName == SharedEmptyHashMap.<String, Set<AttributeModifier>>instance()) {
            this.mapByName = new Object2ObjectOpenHashMap<>();
        }
        if (this.mapByUUID == SharedEmptyHashMap.<UUID, AttributeModifier>instance()) {
            this.mapByUUID = new Object2ObjectOpenHashMap<>();
        }
    }
}
