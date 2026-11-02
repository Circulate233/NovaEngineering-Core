package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.entity.EnumCreatureAttribute;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumCreatureAttribute.class, remap = false)
public abstract class MixinEnumCreatureAttribute {

    @Shadow
    @Final
    private static EnumCreatureAttribute[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumCreatureAttribute[] values() {
        return $VALUES;
    }
}
