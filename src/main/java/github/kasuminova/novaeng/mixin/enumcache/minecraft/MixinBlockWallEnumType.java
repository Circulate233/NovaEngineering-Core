package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockWall;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockWall.EnumType.class, remap = false)
public abstract class MixinBlockWallEnumType {

    @Shadow
    @Final
    private static BlockWall.EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockWall.EnumType[] values() {
        return $VALUES;
    }
}
