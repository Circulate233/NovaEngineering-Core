package github.kasuminova.novaeng.mixin.enumcache.artisanworktables;

import com.codetaylor.mc.artisanworktables.api.tool.reference.EnumWorktableToolType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumWorktableToolType.class, remap = false)
public abstract class MixinEnumWorktableToolType {

    @Shadow
    @Final
    private static EnumWorktableToolType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumWorktableToolType[] values() {
        return $VALUES;
    }
}
