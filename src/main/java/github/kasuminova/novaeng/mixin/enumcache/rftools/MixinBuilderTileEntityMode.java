package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.blocks.builder.BuilderTileEntityMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BuilderTileEntityMode.class, remap = false)
public abstract class MixinBuilderTileEntityMode {

    @Shadow
    @Final
    private static BuilderTileEntityMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BuilderTileEntityMode[] values() {
        return $VALUES;
    }
}
