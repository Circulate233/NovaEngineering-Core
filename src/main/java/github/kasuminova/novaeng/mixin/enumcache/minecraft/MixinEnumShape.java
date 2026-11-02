package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockStairs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockStairs.EnumShape.class, remap = false)
public abstract class MixinEnumShape {

    @Shadow
    @Final
    private static BlockStairs.EnumShape[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockStairs.EnumShape[] values() {
        return $VALUES;
    }
}
