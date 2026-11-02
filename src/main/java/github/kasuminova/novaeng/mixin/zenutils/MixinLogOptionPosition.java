package github.kasuminova.novaeng.mixin.zenutils;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import youyihj.zenutils.api.logger.ILogFilter;
import youyihj.zenutils.api.logger.LogOption;
import youyihj.zenutils.api.logger.ScriptPosition;

import java.util.Set;

/**
 * Only log filters consume the script position; ordinary logging needs no stack trace.
 */
@Mixin(value = LogOption.class, remap = false)
public abstract class MixinLogOptionPosition {

    @Shadow
    @Final
    private Set<ILogFilter> filters;

    @Redirect(method = "isBlock",
        at = @At(value = "INVOKE",
            target = "Lyouyihj/zenutils/api/logger/ScriptPosition;current()Lyouyihj/zenutils/api/logger/ScriptPosition;"),
        require = 1)
    private ScriptPosition nova$positionOnlyForFilters() {
        return this.filters.isEmpty() ? null : ScriptPosition.current();
    }
}
