package github.kasuminova.novaeng.mixin.konkrete;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Copies a localisation file line by line instead of concatenating it into one string first.
 *
 * <p>The original accumulates the whole file with {@code full = full + line + "\n"} inside the read loop. Each
 * iteration builds a fresh builder, appends the entire string collected so far and then a copy of it is handed back,
 * so a file of {@code n} lines costs O(n²) bytes copied - and the finished string is written straight to the
 * buffered writer, which copies it a further time. FancyMenu unpacks every bundled translation this way during
 * client setup, and the copies are the largest single contributor to the allocation rate of that phase.</p>
 *
 * <p>Writing each line as it is read produces the same bytes - the writer is {@code UTF-8} and {@code '\n'} is the
 * same separator the original appended - while touching each character once. The null handling around the reader and
 * writer is kept as it was, including the fact that a failure to open the source reports through
 * {@code printStackTrace} and leaves whatever was already created closed.</p>
 */
@Mixin(targets = "de.keksuccino.konkrete.localization.Locals", remap = false)
public abstract class MixinLocalsCopy {

    @Overwrite(remap = false)
    public static void copyLocalsFileToDir(final ResourceLocation file, final String language,
                                           final String saveDirWithoutFilename) {
        final File lang = new File(saveDirWithoutFilename + "/" + language + ".local");
        if (lang.exists()) {
            lang.delete();
        }

        BufferedReader br = null;
        BufferedWriter bw = null;

        try {
            try {
                br = new BufferedReader(new InputStreamReader(
                    Minecraft.getMinecraft().getResourceManager().getResource(file).getInputStream(),
                    StandardCharsets.UTF_8));
                bw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(lang, false), StandardCharsets.UTF_8));

                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    bw.write(line);
                    bw.write('\n');
                }

                bw.flush();
            } finally {
                bw.close();
                br.close();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
