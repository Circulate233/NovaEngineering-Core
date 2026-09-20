package github.kasuminova.novaeng.mixin.crafttweaker;

import crafttweaker.preprocessor.IPreprocessor;
import crafttweaker.preprocessor.PreprocessorFactory;
import crafttweaker.preprocessor.PreprocessorManager;
import crafttweaker.runtime.ScriptFile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mixin(value = PreprocessorManager.class, remap = false)
public abstract class MixinPreprocessorManager {

    @Shadow
    private HashMap<String, PreprocessorFactory<?>> registeredPreprocessorActions;

    @Shadow
    public HashMap<String, List<IPreprocessor>> preprocessorActionsPerFile;

    @Shadow
    private void addPreprocessorToFileMap(final String filename, final IPreprocessor preprocessor) {
        throw new AssertionError();
    }

    @Overwrite(remap = false)
    private IPreprocessor checkLine(final ScriptFile scriptFile, final String line, final int lineIndex) {
        final String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.charAt(0) != '#') {
            return null;
        }

        final String[] splits = trimmed.substring(1).split(" ");
        if (splits.length == 0) {
            return null;
        }

        final PreprocessorFactory<?> factory = registeredPreprocessorActions.get(splits[0]);
        if (factory == null) {
            return null;
        }

        final IPreprocessor preprocessor = factory.createPreprocessor(scriptFile.getName(), line, lineIndex);
        preprocessor.executeActionOnFind(scriptFile);
        addPreprocessorToFileMap(scriptFile.getGroupName(), preprocessor);
        return preprocessor;
    }

    @Overwrite(remap = false)
    private void executePostActions(final ScriptFile scriptFile) {
        final List<IPreprocessor> preprocessors = preprocessorActionsPerFile.get(scriptFile.getGroupName());
        if (preprocessors == null) {
            return;
        }

        for (final IPreprocessor preprocessor : new ArrayList<>(preprocessors)) {
            preprocessor.executeActionOnFinish(scriptFile);
        }
    }
}
