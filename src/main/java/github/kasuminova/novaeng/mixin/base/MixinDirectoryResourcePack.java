package github.kasuminova.novaeng.mixin.base;

import com.teamacronymcoders.base.util.files.DirectoryResourcePack;
import github.kasuminova.novaeng.client.resource.DirectoryExistenceCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.util.Map;

/**
 * Answers directory backed resource probes from the reload scoped existence cache.
 *
 * <p>The pack resolves every probe through a fresh {@code File} and asks the filesystem whether it exists.
 * During a model reload the same paths are probed over and over, so the answer is memoised for the duration
 * of one reload generation. When no generation is active the original check runs unchanged, so behaviour
 * outside a reload is identical to the original.</p>
 *
 * <p>The target method overrides a Minecraft method and the released jar keeps its obfuscated name, while a
 * development workspace resolves it under the readable name. Both are listed because this class is injected
 * by name and therefore produces no refmap entry to translate between them.</p>
 */
@Mixin(DirectoryResourcePack.class)
public class MixinDirectoryResourcePack {

    @Redirect(method = "hasResourceName", at = @At(value = "INVOKE",
        target = "Ljava/io/File;exists()Z", remap = false), require = 1)
    private boolean nova$cachedExists(final File file) {
        final Map<String, Boolean> generation = DirectoryExistenceCache.instance().active();
        if (generation == null) {
            return file.exists();
        }

        final String key = file.getAbsolutePath();
        final Boolean known = generation.get(key);
        if (known != null) {
            return known;
        }

        final boolean exists = file.exists();
        generation.put(key, exists);
        return exists;
    }
}
