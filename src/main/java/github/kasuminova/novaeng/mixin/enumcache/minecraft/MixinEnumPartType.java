package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockBed;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockBed.EnumPartType.class, remap = false)
public abstract class MixinEnumPartType {

    @Shadow
    @Final
    private static BlockBed.EnumPartType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockBed.EnumPartType[] values() {
        return $VALUES;
    }
}
