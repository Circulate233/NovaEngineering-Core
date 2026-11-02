package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockFlower;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockFlower.EnumFlowerType.class, remap = false)
public abstract class MixinBlockFlowerEnumFlowerType {

    @Shadow
    @Final
    private static BlockFlower.EnumFlowerType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockFlower.EnumFlowerType[] values() {
        return $VALUES;
    }
}
