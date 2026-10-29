package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.item.EnumAction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumAction.class, remap = false)
public abstract class MixinEnumAction {

    @Shadow
    @Final
    private static EnumAction[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumAction[] values() {
        return $VALUES;
    }
}
