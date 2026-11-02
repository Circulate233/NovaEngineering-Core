package github.kasuminova.novaeng.mixin.geckolib3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib3.geo.raw.pojo.Converter;
import software.bernie.shadowed.fasterxml.jackson.databind.ObjectReader;
import software.bernie.shadowed.fasterxml.jackson.databind.ObjectWriter;

@Mixin(value = Converter.class, remap = false)
public abstract class MixinGeckoConverter {
    @Shadow
    private static ObjectReader reader;
    @Shadow
    private static ObjectWriter writer;
    @Unique
    private static volatile boolean nova$mapperReady;

    @Shadow
    private static void instantiateMapper() {
        throw new AssertionError();
    }

    @Unique
    private static void nova$ensureMapper() {
        if (!nova$mapperReady) {
            synchronized (Converter.class) {
                if (!nova$mapperReady) {
                    instantiateMapper();
                    // Publish both fully configured accessors; failed initialization remains retryable.
                    nova$mapperReady = true;
                }
            }
        }
    }

    /**
     * @author circulation
     * @reason Safely share the configured mapper during parallel first use without locking subsequent reads.
     */
    @Overwrite
    private static ObjectReader getObjectReader() {
        nova$ensureMapper();
        return reader;
    }

    /**
     * @author circulation
     * @reason Use the same publication barrier and initialization as the JSON reader.
     */
    @Overwrite
    private static ObjectWriter getObjectWriter() {
        nova$ensureMapper();
        return writer;
    }
}
