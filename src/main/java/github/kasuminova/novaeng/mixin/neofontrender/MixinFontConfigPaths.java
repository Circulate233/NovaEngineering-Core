package github.kasuminova.novaeng.mixin.neofontrender;

import github.kasuminova.novaeng.client.font.BoundedStringLists;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.internal.nightconfig.core.file.CommentedFileConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = NeofontrenderConfig.class, remap = false)
public abstract class MixinFontConfigPaths {
    @Unique
    private static final BoundedStringLists nova$paths = new BoundedStringLists();

    @Redirect(method = {"getFloat(Lneofontrender/internal/nightconfig/core/file/CommentedFileConfig;Ljava/lang/String;F)F",
        "getInt(Lneofontrender/internal/nightconfig/core/file/CommentedFileConfig;Ljava/lang/String;I)I"},
        at = @At(value = "INVOKE", target = "Lneofontrender/internal/nightconfig/core/file/CommentedFileConfig;get(Ljava/lang/String;)Ljava/lang/Object;"),
        require = 2)
    private static Object nova$parsedConfigPath(final CommentedFileConfig config, final String path) {
        return config.get(nova$paths.path(path));
    }
}
