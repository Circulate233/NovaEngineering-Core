package github.kasuminova.novaeng.mixin.enumcache.actuallyadditions;

import de.ellpeck.actuallyadditions.mod.blocks.metalists.TheColoredLampColors;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TheColoredLampColors.class, remap = false)
public abstract class MixinTheColoredLampColors {

    @Shadow
    @Final
    private static TheColoredLampColors[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TheColoredLampColors[] values() {
        return $VALUES;
    }
}
