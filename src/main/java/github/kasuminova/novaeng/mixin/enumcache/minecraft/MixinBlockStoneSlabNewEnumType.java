package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockStoneSlabNew;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockStoneSlabNew.EnumType.class, remap = false)
public abstract class MixinBlockStoneSlabNewEnumType {

    @Shadow
    @Final
    private static BlockStoneSlabNew.EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockStoneSlabNew.EnumType[] values() {
        return $VALUES;
    }
}
