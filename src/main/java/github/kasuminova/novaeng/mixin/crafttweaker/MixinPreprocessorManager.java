package github.kasuminova.novaeng.mixin.crafttweaker;

import crafttweaker.preprocessor.IPreprocessor;
import crafttweaker.preprocessor.PreprocessorFactory;
import crafttweaker.preprocessor.PreprocessorManager;
import crafttweaker.runtime.ScriptFile;
import github.kasuminova.novaeng.common.crafttweaker.util.ScriptDirective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.List;

@Mixin(value = PreprocessorManager.class, remap = false)
public abstract class MixinPreprocessorManager {

    @Shadow
    // CraftTweaker declares concrete HashMap fields; Mixin requires the same descriptors.
    private HashMap<String, PreprocessorFactory<?>> registeredPreprocessorActions;

    @Shadow
    public HashMap<String, List<IPreprocessor>> preprocessorActionsPerFile;

    @Shadow
    private void addPreprocessorToFileMap(final String filename, final IPreprocessor preprocessor) {
        throw new AssertionError();
    }

    /**
     * @author circulation
     * @reason Skip non-directive lines early and register recognised preprocessors by script group.
     */
    @Overwrite(remap = false)
    private IPreprocessor checkLine(final ScriptFile scriptFile, final String line, final int lineIndex) {
        final String directive = ScriptDirective.name(line);
        if (directive == null) {
            return null;
        }
        final PreprocessorFactory<?> factory = registeredPreprocessorActions.get(directive);
        if (factory == null) {
            return null;
        }

        final IPreprocessor preprocessor = factory.createPreprocessor(scriptFile.getName(), line, lineIndex);
        preprocessor.executeActionOnFind(scriptFile);
        addPreprocessorToFileMap(scriptFile.getGroupName(), preprocessor);
        return preprocessor;
    }

    /**
     * @author circulation
     * @reason Execute a snapshot so post-actions can modify the registered preprocessor list safely.
     */
    @Overwrite(remap = false)
    private void executePostActions(final ScriptFile scriptFile) {
        final List<IPreprocessor> preprocessors = preprocessorActionsPerFile.get(scriptFile.getGroupName());
        if (preprocessors == null) {
            return;
        }

        // Post-actions may mutate the source list. One array preserves that snapshot
        // contract without a temporary list wrapper and its iterator.
        for (final IPreprocessor preprocessor : preprocessors.toArray(new IPreprocessor[preprocessors.size()])) {
            preprocessor.executeActionOnFinish(scriptFile);
        }
    }
}
