package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockTallGrass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockTallGrass.EnumType.class, remap = false)
public abstract class MixinBlockTallGrassEnumType {

    @Shadow
    @Final
    private static BlockTallGrass.EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockTallGrass.EnumType[] values() {
        return $VALUES;
    }
}
