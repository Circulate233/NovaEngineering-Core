package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockFlowerPot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockFlowerPot.EnumFlowerType.class, remap = false)
public abstract class MixinBlockFlowerPotEnumFlowerType {

    @Shadow
    @Final
    private static BlockFlowerPot.EnumFlowerType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockFlowerPot.EnumFlowerType[] values() {
        return $VALUES;
    }
}
