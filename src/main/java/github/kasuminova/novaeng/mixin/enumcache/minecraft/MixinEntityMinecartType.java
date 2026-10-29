package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.entity.item.EntityMinecart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EntityMinecart.Type.class, remap = false)
public abstract class MixinEntityMinecartType {

    @Shadow
    @Final
    private static EntityMinecart.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EntityMinecart.Type[] values() {
        return $VALUES;
    }
}
