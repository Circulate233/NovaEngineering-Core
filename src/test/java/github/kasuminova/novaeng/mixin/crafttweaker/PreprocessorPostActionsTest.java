package github.kasuminova.novaeng.mixin.crafttweaker;

import crafttweaker.preprocessor.IPreprocessor;
import crafttweaker.runtime.ScriptFile;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreprocessorPostActionsTest {
    private static IPreprocessor action(final Runnable callback) {
        return (IPreprocessor) Proxy.newProxyInstance(IPreprocessor.class.getClassLoader(), new Class<?>[]{IPreprocessor.class},
            (proxy, method, args) -> {
                if (method.getName().equals("executeActionOnFinish")) callback.run();
                return null;
            });
    }

    @Test
    void sourceMutationDuringCallbackDoesNotChangeTheCurrentSnapshot() throws Exception {
        final var manager = new MixinPreprocessorManager() { };
        final List<String> events = new ObjectArrayList<>();
        final List<IPreprocessor> source = new ObjectArrayList<>();
        final IPreprocessor third = action(() -> events.add("third"));
        final IPreprocessor first = action(() -> {
            events.add("first");
            source.clear();
            source.add(third);
        });
        final IPreprocessor second = action(() -> events.add("second"));
        source.add(first);
        source.add(second);
        // The field's concrete HashMap descriptor belongs to CraftTweaker's target class.
        manager.preprocessorActionsPerFile = new HashMap<>();
        manager.preprocessorActionsPerFile.put("group", source);
        final ScriptFile script = new ScriptFile(null, null, false) {
            @Override
            public String getGroupName() {
                return "group";
            }
        };
        final Method execute = MixinPreprocessorManager.class.getDeclaredMethod("executePostActions", ScriptFile.class);
        execute.setAccessible(true);
        execute.invoke(manager, script);
        assertEquals(List.of("first", "second"), events);
        execute.invoke(manager, script);
        assertEquals(List.of("first", "second", "third"), events);
    }
}
