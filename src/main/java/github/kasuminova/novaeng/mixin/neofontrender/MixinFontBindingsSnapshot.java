package github.kasuminova.novaeng.mixin.neofontrender;

import github.kasuminova.novaeng.client.font.TooltipBindings;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.internal.nightconfig.core.file.CommentedFileConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(value = NeofontrenderConfig.class, remap = false)
public abstract class MixinFontBindingsSnapshot {
    @Unique
    private static final TooltipBindings nova$bindings = new TooltipBindings();
    @Unique
    private static final List<String> nova$bindingsPath = List.of("brilliant", "bindings");
    @Shadow
    private static CommentedFileConfig config;
    @Shadow
    @Final
    private static List<String> DEFAULT_BRILLIANT_BINDINGS;

    /**
     * @author NovaEngineering
     * @reason Reuse the sanitized string list while observing in-place edits on every read.
     */
    @Overwrite
    public static List<String> brilliantTextBindings() {
        return nova$bindings.resolve(config == null ? null : config.get(nova$bindingsPath), DEFAULT_BRILLIANT_BINDINGS);
    }
}
