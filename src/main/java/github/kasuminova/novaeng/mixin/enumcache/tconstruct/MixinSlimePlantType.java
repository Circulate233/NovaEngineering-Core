package github.kasuminova.novaeng.mixin.enumcache.tconstruct;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.world.block.BlockTallSlimeGrass.SlimePlantType;

@Mixin(value = SlimePlantType.class, remap = false)
public abstract class MixinSlimePlantType {

    @Shadow
    @Final
    private static SlimePlantType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SlimePlantType[] values() {
        return $VALUES;
    }
}
