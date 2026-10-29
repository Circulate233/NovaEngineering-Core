package github.kasuminova.novaeng.mixin.enumcache.actuallyadditions;

import de.ellpeck.actuallyadditions.mod.blocks.metalists.TheMiscBlocks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TheMiscBlocks.class, remap = false)
public abstract class MixinTheMiscBlocks {

    @Shadow
    @Final
    private static TheMiscBlocks[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TheMiscBlocks[] values() {
        return $VALUES;
    }
}
