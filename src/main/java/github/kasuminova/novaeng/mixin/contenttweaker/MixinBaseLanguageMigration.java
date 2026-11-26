package github.kasuminova.novaeng.mixin.contenttweaker;

import com.teamacronymcoders.base.util.files.BaseFileUtils;
import com.teamacronymcoders.base.util.files.ResourceLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.util.regex.Pattern;

@Mixin(value = ResourceLoader.class, remap = false)
public abstract class MixinBaseLanguageMigration {
    @Shadow
    Pattern pattern;

    /**
     * @author NovaEngineering
     * @reason Skip writing valid, nonempty language files whose migration did not change their contents.
     */
    @Overwrite
    private void fixLangFile(final File file) {
        final String contents = BaseFileUtils.readFileToString(file);
        final String migrated = pattern.matcher(contents).replaceAll("=");
        // The reader also returns empty on I/O failure and replaces malformed UTF-8.
        if (contents.isEmpty() || contents.indexOf('\uFFFD') >= 0 || !contents.equals(migrated)) {
            BaseFileUtils.writeStringToFile(migrated, file);
        }
    }
}
