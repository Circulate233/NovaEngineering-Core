package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.entity.item.EntityPainting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EntityPainting.EnumArt.class, remap = false)
public abstract class MixinEnumArt {

    @Shadow
    @Final
    private static EntityPainting.EnumArt[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EntityPainting.EnumArt[] values() {
        return $VALUES;
    }
}
