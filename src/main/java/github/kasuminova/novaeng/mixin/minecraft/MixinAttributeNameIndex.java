package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.AttributeNameIndex;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.LowerStringMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({AbstractAttributeMap.class, AttributeMap.class})
public abstract class MixinAttributeNameIndex {
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/util/LowerStringMap"), require = 1)
    private LowerStringMap<IAttributeInstance> nova$ownedNameIndex() {
        return new AttributeNameIndex<>();
    }
}
