package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockPistonExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockPistonExtension.EnumPistonType.class, remap = false)
public abstract class MixinEnumPistonType {

    @Shadow
    @Final
    private static BlockPistonExtension.EnumPistonType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockPistonExtension.EnumPistonType[] values() {
        return $VALUES;
    }
}
