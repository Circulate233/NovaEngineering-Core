package github.kasuminova.novaeng.common.logging.agent;

import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/** Replaces logging entry points before message construction, filtering or appender dispatch. */
public final class LoggingMethodTransformer implements ClassFileTransformer {
    private static final ClassDesc LOG_BUILDER = ClassDesc.of("org.apache.logging.log4j.LogBuilder");
    private volatile RuntimeException failure;

    public RuntimeException getFailure() {
        return failure;
    }

    public static boolean isTarget(final String name) {
        if (name == null) return false;
        return switch (name) {
            case "org/apache/logging/log4j/spi/AbstractLogger",
                 "org/apache/logging/log4j/spi/ExtendedLoggerWrapper",
                 "org/apache/logging/log4j/core/Logger",
                 "org/apache/logging/log4j/core/async/AsyncLogger",
                 "org/apache/logging/log4j/simple/SimpleLogger",
                 "org/apache/logging/log4j/status/StatusLogger",
                 "org/apache/logging/log4j/internal/DefaultLogBuilder",
                 "org/apache/logging/log4j/EventLogger",
                 "java/util/logging/Logger",
                 "net/minecraft/launchwrapper/LogWrapper",
                 "net/minecraftforge/fml/common/FMLLog",
                 "net/minecraftforge/fml/relauncher/FMLRelaunchLog",
                 "org/spongepowered/asm/logging/LoggerAdapterAbstract",
                 "org/spongepowered/asm/logging/LoggerAdapterConsole",
                 "org/spongepowered/asm/logging/LoggerAdapterJava",
                 "org/spongepowered/asm/logging/LoggerAdapterDefault",
                 "org/spongepowered/asm/service/mojang/LoggerAdapterLog4j2" -> true;
            default -> false;
        };
    }

    @Override
    public byte[] transform(final ClassLoader loader, final String name, final Class<?> redefined,
                            final ProtectionDomain domain, final byte[] bytes) {
        if (!isTarget(name)) return null;
        try {
            return rewrite(name, bytes);
        } catch (final RuntimeException exception) {
            if (failure == null) {
                synchronized (this) {
                    if (failure == null) failure = exception;
                }
            }
            throw exception;
        }
    }

    private static byte[] rewrite(final String name, final byte[] bytes) {
        final ClassFile classFile = ClassFile.of();
        return classFile.transformClass(classFile.parse(bytes), (builder, element) -> {
            if (element instanceof MethodModel method && method.code().isPresent()) {
                final Body body = replacement(name, method);
                if (body != null) {
                    builder.transformMethod(method, (methodBuilder, methodElement) -> {
                        if (methodElement instanceof CodeModel) {
                            methodBuilder.withCode(code -> writeBody(code, method, body));
                        } else {
                            methodBuilder.with(methodElement);
                        }
                    });
                    return;
                }
            }
            builder.with(element);
        });
    }

    private static Body replacement(final String owner, final MethodModel method) {
        final String name = method.methodName().stringValue();
        final MethodTypeDesc type = method.methodTypeSymbol();
        final TypeKind result = TypeKind.from(type.returnType());
        if (result == TypeKind.BOOLEAN && (name.equals("isEnabled") || name.equals("isLoggable")
            || name.startsWith("is") && name.endsWith("Enabled"))) return Body.FALSE;
        if (type.returnType().equals(LOG_BUILDER) && name.startsWith("at")) return Body.BUILDER;
        if (result != TypeKind.VOID && (name.equals("throwing") || name.equals("exit") || name.equals("traceExit"))) {
            final int last = type.parameterCount() - 1;
            if (last < 0 || !type.parameterType(last).equals(type.returnType())) {
                throw new IllegalArgumentException("Unrecognized logging result: " + owner + "." + name + type);
            }
            return Body.RESULT;
        }
        if (result == TypeKind.REFERENCE && (name.equals("traceEntry") || name.equals("enter")
            || name.equals("entryMsg") || name.equals("logAndGet"))) return Body.NULL;
        if (result != TypeKind.VOID) return null;
        return switch (name) {
            case "trace", "debug", "info", "warn", "error", "fatal", "log", "logIfEnabled", "logMessage",
                 "printf", "entry", "exit", "traceExit", "catching", "throwing", "logEvent",
                 "logMessageInCurrentThread", "actualAsyncLog", "logp", "logrb", "entering", "exiting",
                 "severe", "warning", "config", "fine", "finer", "finest", "bigWarning", "makeLog" -> Body.VOID;
            default -> null;
        };
    }

    private static void writeBody(final CodeBuilder code, final MethodModel method, final Body body) {
        switch (body) {
            case VOID -> code.return_();
            case FALSE -> code.iconst_0().ireturn();
            case NULL -> code.aconst_null().areturn();
            case BUILDER -> code.getstatic(LOG_BUILDER, "NOOP", LOG_BUILDER).areturn();
            case RESULT -> {
                final MethodTypeDesc type = method.methodTypeSymbol();
                final int last = type.parameterCount() - 1;
                final TypeKind kind = TypeKind.from(type.returnType());
                code.loadLocal(kind, code.parameterSlot(last)).return_(kind);
            }
        }
    }

    private enum Body { VOID, FALSE, NULL, BUILDER, RESULT }
}
