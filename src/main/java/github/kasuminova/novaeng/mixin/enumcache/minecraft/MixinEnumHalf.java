package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockStairs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockStairs.EnumHalf.class, remap = false)
public abstract class MixinEnumHalf {

    @Shadow
    @Final
    private static BlockStairs.EnumHalf[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockStairs.EnumHalf[] values() {
        return $VALUES;
    }
}
