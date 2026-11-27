package github.kasuminova.novaeng.common.logging.agent;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;

/** Installs before Foundation's parent-loaded loggers, without loading Forge or Log4j. */
public final class LoggingSilencerAgent {
    private LoggingSilencerAgent() {
    }

    public static void premain(final String options, final Instrumentation instrumentation) throws Exception {
        if (!LoggingAgentConfig.isEnabled(options)) return;
        if (!instrumentation.isRetransformClassesSupported()) {
            throw new IllegalStateException("DisableLogOutput requires class retransformation support");
        }
        final LoggingMethodTransformer transformer = new LoggingMethodTransformer();
        instrumentation.addTransformer(transformer, true);
        // Other JVM agents may have initialized loggers before this premain. Rewrite those same
        // classes so already-published static final Logger references also become no-ops.
        final Class<?>[] loaded = instrumentation.getAllLoadedClasses();
        int loadedCount = 0;
        for (final Class<?> type : loaded) {
            if (!LoggingMethodTransformer.isTarget(type.getName().replace('.', '/'))) continue;
            if (!instrumentation.isModifiableClass(type)) {
                throw new IllegalStateException("Cannot disable logging in unmodifiable class " + type.getName());
            }
            loaded[loadedCount++] = type;
        }
        if (loadedCount != 0) {
            instrumentation.retransformClasses(loadedCount == loaded.length ? loaded : Arrays.copyOf(loaded, loadedCount));
        }
        // Instrumentation ignores transformer exceptions, so check before claiming success.
        if (transformer.getFailure() != null) {
            throw new IllegalStateException("Could not replace logging methods", transformer.getFailure());
        }
        System.setProperty("novaeng.logOutputSuppressed", "true");
    }
}
