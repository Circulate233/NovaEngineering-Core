package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import github.kasuminova.novaeng.common.performance.RecipeJarProbe;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.ModContainer;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mixin(value = CraftingHelper.class, remap = false)
public abstract class MixinCraftingHelperJarScan {

    @Shadow
    private static void loadFactories(final JsonObject json, final JsonContext context,
                                      final CraftingHelper.FactoryLoader<?>... loaders) {
        throw new AssertionError();
    }

    @Redirect(method = "loadFactories(Lnet/minecraftforge/fml/common/ModContainer;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/common/crafting/CraftingHelper;loadFactories(Lnet/minecraftforge/fml/common/ModContainer;Ljava/lang/String;[Lnet/minecraftforge/common/crafting/CraftingHelper$FactoryLoader;)V"),
        require = 1)
    private static void nova$readFactoryEntry(final ModContainer mod, final String base,
                                              final CraftingHelper.FactoryLoader<?>[] loaders) {
        final File source = mod.getResource();
        if (!source.getName().endsWith(".jar") || !source.isFile()) {
            CraftingHelper.loadFactories(mod, base, loaders);
            return;
        }
        ZipFile zip = null;
        try {
            zip = new ZipFile(source);
            // ZIPFS rejects some malformed entry paths even when _factories.json is absent.
            // Preserve that provider validation and its logging for unconventional archives.
            if (!RecipeJarProbe.isConventional(source, zip)) {
                CraftingHelper.loadFactories(mod, base, loaders);
                return;
            }
            final ZipEntry entry = zip.getEntry(base + "/_factories.json");
            if (entry == null) {
                return;
            }
            if (entry.isDirectory()) {
                CraftingHelper.loadFactories(mod, base, loaders);
                return;
            }
            // Files.newBufferedReader reports malformed UTF-8 instead of replacing it.
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                zip.getInputStream(entry), StandardCharsets.UTF_8.newDecoder()))) {
                final JsonObject json = JsonUtils.fromJson(CraftingHelper.GSON, reader, JsonObject.class);
                loadFactories(json, new JsonContext(mod.getModId()), loaders);
            }
        } catch (final JsonParseException | IOException failure) {
            FMLLog.log.error("Error loading _factories.json: ", failure);
        } finally {
            IOUtils.closeQuietly(zip);
        }
    }

    @Redirect(method = "findFiles(Lnet/minecraftforge/fml/common/ModContainer;Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/BiFunction;ZZ)Z", at = @At(value = "INVOKE",
        target = "Ljava/io/File;isFile()Z"),
        require = 1)
    private static boolean nova$probeRecipeRoot(final File source, final ModContainer mod, final String base,
                                                final Function<Path, Boolean> preprocessor,
                                                final BiFunction<Path, Path, Boolean> processor,
                                                final boolean defaultUnfoundRoot, final boolean visitAllFiles) {
        // A known absent archive root follows the original null-root/default-result path without callbacks.
        return source.isFile() && (!source.getName().endsWith(".jar") || RecipeJarProbe.mayContain(source, base));
    }

    @Redirect(method = "lambda$loadRecipes$2", at = @At(value = "INVOKE",
        target = "Ljava/lang/String;replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"), require = 1)
    private static String nova$recipePathSeparators(final String path, final String regex, final String replacement) {
        return path.replace('\\', '/');
    }
}
