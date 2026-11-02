package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockLever;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockLever.EnumOrientation.class, remap = false)
public abstract class MixinEnumOrientation {

    @Shadow
    @Final
    private static BlockLever.EnumOrientation[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockLever.EnumOrientation[] values() {
        return $VALUES;
    }
}
