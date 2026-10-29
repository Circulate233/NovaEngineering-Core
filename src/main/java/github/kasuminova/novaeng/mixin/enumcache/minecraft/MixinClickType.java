package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.inventory.ClickType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ClickType.class, remap = false)
public abstract class MixinClickType {

    @Shadow
    @Final
    private static ClickType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ClickType[] values() {
        return $VALUES;
    }
}
