package github.kasuminova.novaeng.mixin.minecraft;

import com.google.common.collect.Maps;
import github.kasuminova.novaeng.common.performance.AttributeNameIndex;
import github.kasuminova.novaeng.common.performance.SharedAttributeNameMap;
import net.minecraft.util.LowerStringMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(LowerStringMap.class)
public abstract class MixinAttributeNameIndexStorage<V> {
    @Shadow
    @Final
    @Mutable
    private Map<String, V> internalMap;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", remap = false,
        target = "Lcom/google/common/collect/Maps;newLinkedHashMap()Ljava/util/LinkedHashMap;"), require = 1)
    private LinkedHashMap<String, V> nova$deferOwnedNameStorage() {
        return (Object) this instanceof AttributeNameIndex<?> ? null : Maps.newLinkedHashMap();
    }

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void nova$installOwnedNameStorage(final CallbackInfo ci) {
        if ((Object) this instanceof AttributeNameIndex<?>) this.internalMap = new SharedAttributeNameMap<>();
    }
}
