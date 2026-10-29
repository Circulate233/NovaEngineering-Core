package github.kasuminova.novaeng.mixin.enumcache.libnine;

import io.github.phantamanta44.libnine.util.world.BlockSide;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockSide.class, remap = false)
public abstract class MixinBlockSide {

    @Shadow
    @Final
    private static BlockSide[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockSide[] values() {
        return $VALUES;
    }
}
