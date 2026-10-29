package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockStone;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockStone.EnumType.class, remap = false)
public abstract class MixinBlockStoneEnumType {

    @Shadow
    @Final
    private static BlockStone.EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockStone.EnumType[] values() {
        return $VALUES;
    }
}
