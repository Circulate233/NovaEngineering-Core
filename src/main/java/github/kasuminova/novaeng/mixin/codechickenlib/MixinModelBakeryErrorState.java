package github.kasuminova.novaeng.mixin.codechickenlib;

import codechicken.lib.internal.CCLLog;
import codechicken.lib.model.bakery.ModelBakery;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.NovaEngineeringCore;
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * CodeChicken Lib reads the model error state of a block state as "the block declares
 * {@code ModelErrorStateProperty.ERROR_STATE} but carries no value for it, so somebody mishandled the model", and
 * answers with a FATAL log per tick from {@code ModelBakery.getCachedModel}.
 * <p>
 * Forge cannot express "declared but unset" any other way: {@code ExtendedBlockState.buildUnlistedMap} registers every
 * declared unlisted property with {@code Optional.empty()}, so {@code getUnlistedProperties().containsKey(ERROR_STATE)}
 * is true for every state of every block that declares the property, and {@code getValue} answers {@code null} until
 * {@code ModelBakery.handleExtendedState} has run and the block's own bakery filled the value in. Callers that bake a
 * raw block state off the render thread therefore hit this branch on every block they visit - JourneyMap's map renderer
 * does exactly that with its worker threads, which is where the measured flood comes from (client render thread never
 * hits it). Such a state also has no {@code LAYER_FACE_SPRITE_MAP}, so the model genuinely cannot be baked and the
 * original answer - {@code missingModel} - is the correct one; only the log is wrong. Report it once instead of every
 * tick, and keep the original behaviour when the optimization is disabled.
 */
@Mixin(value = ModelBakery.class, remap = false)
public abstract class MixinModelBakeryErrorState {

    @Unique
    private static boolean nova$reportedUnhandledErrorState;

    @Redirect(method = "getCachedModel", at = @At(value = "INVOKE",
            target = "Lcodechicken/lib/internal/CCLLog;logOncePerTick(Lorg/apache/logging/log4j/Level;Ljava/lang/String;[Ljava/lang/Object;)V",
            ordinal = 0), remap = false, require = 1)
    private static void nova$reportUnhandledErrorStateOnce(final Level level, final String message, final Object[] args) {
        if (!NovaEngCoreConfig.CLIENT.optimizeCodeChickenErrorStateLog) {
            CCLLog.logOncePerTick(level, message, args);
            return;
        }
        if (nova$reportedUnhandledErrorState) {
            return;
        }
        nova$reportedUnhandledErrorState = true;
        NovaEngineeringCore.log.warn("A block state was baked without a value for ModelErrorStateProperty.ERROR_STATE," +
                " which CodeChickenLib logs as a FATAL once per tick. The state was never passed through" +
                " ModelBakery.handleExtendedState, so it has no LAYER_FACE_SPRITE_MAP either and cannot be baked;" +
                " this is what JourneyMap's map renderer does from its worker threads. The models stay unrendered, the" +
                " log spam is suppressed.");
    }
}
