package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.EnumBlockRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumBlockRenderType.class, remap = false)
public abstract class MixinEnumBlockRenderType {

    @Shadow
    @Final
    private static EnumBlockRenderType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumBlockRenderType[] values() {
        return $VALUES;
    }
}
