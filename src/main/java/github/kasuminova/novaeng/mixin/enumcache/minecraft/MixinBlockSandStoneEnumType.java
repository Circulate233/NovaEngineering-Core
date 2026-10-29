package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockSandStone;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockSandStone.EnumType.class, remap = false)
public abstract class MixinBlockSandStoneEnumType {

    @Shadow
    @Final
    private static BlockSandStone.EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockSandStone.EnumType[] values() {
        return $VALUES;
    }
}
