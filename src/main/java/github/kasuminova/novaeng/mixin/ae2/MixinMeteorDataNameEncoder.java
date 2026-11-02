package github.kasuminova.novaeng.mixin.ae2;

import appeng.core.worlddata.MeteorDataNameEncoder;
import github.kasuminova.novaeng.common.performance.MeteorDataFilename;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MeteorDataNameEncoder.class, remap = false)
public abstract class MixinMeteorDataNameEncoder {

    @Shadow
    @Final
    private char dataSeparator;

    @Shadow
    @Final
    private String fileExtension;

    @Shadow
    @Final
    private char baseExtSeparator;

    @Shadow
    @Final
    private int bitScale;

    /**
     * @author circulation
     * @reason Build decimal region filenames without Formatter's parsing and varargs on ASCII-digit locales.
     */
    @Overwrite
    public String encode(final int dimension, final int chunkX, final int chunkZ) {
        return MeteorDataFilename.encode(dimension, chunkX, chunkZ, this.bitScale,
            this.dataSeparator, this.baseExtSeparator, this.fileExtension);
    }
}
