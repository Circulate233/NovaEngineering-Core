package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockQuartz;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockQuartz.EnumType.class, remap = false)
public abstract class MixinBlockQuartzEnumType {

    @Shadow
    @Final
    private static BlockQuartz.EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockQuartz.EnumType[] values() {
        return $VALUES;
    }
}
