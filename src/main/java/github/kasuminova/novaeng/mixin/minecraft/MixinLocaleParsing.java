package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.LanguageLineParser;
import net.minecraft.client.resources.Locale;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Mixin(Locale.class)
public abstract class MixinLocaleParsing {
    @Shadow
    Map<String, String> properties;

    /**
     * @author circulation
     * @reason Preserve Forge's language hook while avoiding split arrays and regex matching for plain values.
     */
    @Overwrite
    private void loadLocaleData(InputStream stream) throws IOException {
        stream = FMLCommonHandler.instance().loadLanguage(properties, stream);
        if (stream != null) {
            // Keep the original read-before-insert behavior if the stream fails partway through.
            for (final String line : IOUtils.readLines(stream, StandardCharsets.UTF_8)) {
                LanguageLineParser.put(properties, line);
            }
        }
    }
}
