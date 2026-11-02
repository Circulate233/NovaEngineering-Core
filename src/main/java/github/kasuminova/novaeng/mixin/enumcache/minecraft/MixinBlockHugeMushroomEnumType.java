package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockHugeMushroom;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockHugeMushroom.EnumType.class, remap = false)
public abstract class MixinBlockHugeMushroomEnumType {

    @Shadow
    @Final
    private static BlockHugeMushroom.EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockHugeMushroom.EnumType[] values() {
        return $VALUES;
    }
}
