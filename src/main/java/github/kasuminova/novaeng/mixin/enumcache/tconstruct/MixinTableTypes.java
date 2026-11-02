package github.kasuminova.novaeng.mixin.enumcache.tconstruct;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.tools.common.block.BlockToolTable.TableTypes;

@Mixin(value = TableTypes.class, remap = false)
public abstract class MixinTableTypes {

    @Shadow
    @Final
    private static TableTypes[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TableTypes[] values() {
        return $VALUES;
    }
}
