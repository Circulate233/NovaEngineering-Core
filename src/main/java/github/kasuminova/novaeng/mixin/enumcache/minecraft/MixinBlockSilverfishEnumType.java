package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockSilverfish;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockSilverfish.EnumType.class, remap = false)
public abstract class MixinBlockSilverfishEnumType {

    @Shadow
    @Final
    private static BlockSilverfish.EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockSilverfish.EnumType[] values() {
        return $VALUES;
    }
}
