package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockDoublePlant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockDoublePlant.EnumBlockHalf.class, remap = false)
public abstract class MixinBlockDoublePlantEnumBlockHalf {

    @Shadow
    @Final
    private static BlockDoublePlant.EnumBlockHalf[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockDoublePlant.EnumBlockHalf[] values() {
        return $VALUES;
    }
}
