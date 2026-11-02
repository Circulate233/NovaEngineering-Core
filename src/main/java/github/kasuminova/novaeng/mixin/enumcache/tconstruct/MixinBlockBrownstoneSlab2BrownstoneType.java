package github.kasuminova.novaeng.mixin.enumcache.tconstruct;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.gadgets.block.BlockBrownstoneSlab2.BrownstoneType;

@Mixin(value = BrownstoneType.class, remap = false)
public abstract class MixinBlockBrownstoneSlab2BrownstoneType {

    @Shadow
    @Final
    private static BrownstoneType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BrownstoneType[] values() {
        return $VALUES;
    }
}
