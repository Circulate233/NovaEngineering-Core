package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.block.BlockSheet.SheetType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SheetType.class, remap = false)
public abstract class MixinSheetType {

    @Shadow
    @Final
    private static SheetType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SheetType[] values() {
        return $VALUES;
    }
}
