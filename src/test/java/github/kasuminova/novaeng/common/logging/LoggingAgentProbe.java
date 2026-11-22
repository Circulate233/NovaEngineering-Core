package github.kasuminova.novaeng.common.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.util.Supplier;
import org.spongepowered.asm.logging.LoggerAdapterConsole;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/** Standalone child process target. No JUnit dependency is needed inside either premain. */
public final class LoggingAgentProbe {
    private static final Logger EXISTING = LogManager.getLogger("novaeng-agent-probe-existing");
    private static final java.util.logging.Logger JUL = java.util.logging.Logger.getLogger("novaeng-agent-probe-jul");
    private static final LoggerAdapterConsole CONSOLE = new LoggerAdapterConsole("novaeng-agent-probe-console");
    private static int events;
    private static int formatted;
    private static int supplied;
    private static int stringified;
    private static int julEvents;
    private static int julSupplied;
    private static boolean initialized;

    private LoggingAgentProbe() {
    }

    public static final class PreloadAgent {
        private PreloadAgent() {
        }

        public static void premain(final String options, final Instrumentation instrumentation) {
            initialize();
        }
    }

    private static void initialize() {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final AbstractAppender appender = new AbstractAppender("probe-counter", null, null, true) {
            @Override
            public void append(final LogEvent event) {
                events++;
                event.getMessage().getFormattedMessage();
            }
        };
        appender.start();
        final LoggerConfig loggerConfig = new LoggerConfig(EXISTING.getName(), Level.ALL, false);
        loggerConfig.addAppender(appender, null, null);
        context.getConfiguration().addLogger(EXISTING.getName(), loggerConfig);
        context.updateLoggers();
        JUL.setUseParentHandlers(false);
        JUL.setLevel(java.util.logging.Level.ALL);
        JUL.addHandler(new Handler() {
            @Override
            public void publish(final LogRecord record) {
                julEvents++;
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        initialized = true;
    }

    public static void main(final String[] arguments) {
        if (!initialized) initialize();
        final boolean suppressed = Boolean.parseBoolean(arguments[0]);
        check(Boolean.getBoolean("novaeng.logOutputSuppressed") == suppressed, "Agent activation flag");
        check(EXISTING.isInfoEnabled() != suppressed, "Existing logger enabled state");
        check(LogManager.getLogger("novaeng-agent-probe-new").isErrorEnabled() != suppressed,
            "New logger enabled state");
        if (suppressed) {
            for (final Level level : Level.values()) {
                check(!EXISTING.isEnabled(level), "Log4j level remained enabled: " + level);
            }
        }

        final Object message = new Object() {
            @Override
            public String toString() {
                stringified++;
                return "formatted-object";
            }
        };
        EXISTING.info("object={}", message);
        EXISTING.info((Supplier<?>) () -> {
            supplied++;
            return "supplier-message";
        });
        EXISTING.atInfo().log((Supplier<Message>) () -> {
            supplied++;
            return new SimpleMessage("builder-message");
        });
        ((ExtendedLogger) EXISTING).logMessage(LoggingAgentProbe.class.getName(), Level.ERROR, null, new Message() {
            @Override
            public String getFormattedMessage() {
                formatted++;
                return "direct-message";
            }

            @Override
            public String getFormat() {
                return "direct-message";
            }

            @Override
            public Object[] getParameters() {
                return null;
            }

            @Override
            public Throwable getThrowable() {
                return null;
            }
        }, null);
        final RuntimeException failure = new RuntimeException("flow");
        final Object result = new Object();
        check(EXISTING.throwing(failure) == failure, "throwing must return the supplied Throwable");
        check(EXISTING.exit(result) == result, "exit must retain its result");
        check(EXISTING.traceExit("result={}", result) == result, "traceExit must retain its result");
        JUL.log(java.util.logging.Level.INFO, () -> {
            julSupplied++;
            return "jul-supplier";
        });
        check(JUL.isLoggable(java.util.logging.Level.SEVERE) != suppressed, "JUL enabled state");

        final ByteArrayOutputStream consoleBytes = new ByteArrayOutputStream();
        final PrintStream previousOut = System.out;
        final PrintStream previousErr = System.err;
        try (final PrintStream capture = new PrintStream(consoleBytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            System.setErr(capture);
            CONSOLE.info("console object={}", message);
            check(CONSOLE.throwing(failure) == failure, "Mixin throwing must return the supplied Throwable");
        } finally {
            System.setOut(previousOut);
            System.setErr(previousErr);
        }
        if (suppressed) {
            check(events == 0, "Appender was called: " + events);
            check(formatted == 0, "Direct Message was formatted");
            check(supplied == 0, "Log4j supplier was evaluated");
            check(stringified == 0, "Log argument toString was evaluated");
            check(julEvents == 0 && julSupplied == 0, "JUL performed logging work");
            check(consoleBytes.size() == 0, "Mixin console emitted output");
        } else {
            check(events >= 4, "Disabled agent unexpectedly suppressed logging");
            check(formatted > 0, "Direct Message was not formatted");
            check(supplied >= 2, "Log4j suppliers were not evaluated");
            check(stringified >= 2, "Object/console logging did not format its arguments");
            check(julEvents == 1 && julSupplied == 1, "Disabled agent suppressed JUL");
            check(consoleBytes.size() > 0, "Disabled agent suppressed Mixin console");
        }
        System.out.println("DIRECT_STDOUT_ALIVE");
        System.out.println("PROBE_OK suppressed=" + suppressed);
    }

    private static void check(final boolean value, final String message) {
        if (!value) throw new AssertionError(message + " [events=" + events + ", formatted=" + formatted
            + ", supplied=" + supplied + ", stringified=" + stringified + ", julEvents=" + julEvents
            + ", julSupplied=" + julSupplied + ']');
    }
}
