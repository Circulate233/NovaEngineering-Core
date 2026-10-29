package github.kasuminova.novaeng.mixin.enumcache.redstonerepository;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import thundr.redstonerepository.init.RedstoneRepositoryEquipment.ToolSet;

@Mixin(value = ToolSet.class, remap = false)
public abstract class MixinToolSet {

    @Shadow
    @Final
    private static ToolSet[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ToolSet[] values() {
        return $VALUES;
    }
}
