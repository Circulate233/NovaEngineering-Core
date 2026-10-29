package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockSand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockSand.EnumType.class, remap = false)
public abstract class MixinBlockSandEnumType {

    @Shadow
    @Final
    private static BlockSand.EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockSand.EnumType[] values() {
        return $VALUES;
    }
}
