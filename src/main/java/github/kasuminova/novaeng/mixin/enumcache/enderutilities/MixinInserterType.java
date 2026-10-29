package github.kasuminova.novaeng.mixin.enumcache.enderutilities;

import fi.dy.masa.enderutilities.block.BlockInserter.InserterType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = InserterType.class, remap = false)
public abstract class MixinInserterType {

    @Shadow
    @Final
    private static InserterType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static InserterType[] values() {
        return $VALUES;
    }
}
