package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockDirt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockDirt.DirtType.class, remap = false)
public abstract class MixinDirtType {

    @Shadow
    @Final
    private static BlockDirt.DirtType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockDirt.DirtType[] values() {
        return $VALUES;
    }
}
