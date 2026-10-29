package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockPlanks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockPlanks.EnumType.class, remap = false)
public abstract class MixinBlockPlanksEnumType {

    @Shadow
    @Final
    private static BlockPlanks.EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockPlanks.EnumType[] values() {
        return $VALUES;
    }
}
