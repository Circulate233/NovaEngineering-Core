package github.kasuminova.novaeng.common.logging.agent;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.Instruction;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.attribute.CodeAttribute;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingMethodTransformerTest {
    private static final String ABSTRACT = "org/apache/logging/log4j/spi/AbstractLogger";

    private static byte[] resource(final String name) throws Exception {
        try (final InputStream input = LoggingMethodTransformerTest.class.getClassLoader()
            .getResourceAsStream(name + ".class")) {
            assertNotNull(input, name);
            return input.readAllBytes();
        }
    }

    private static ClassModel transform(final String name) throws Exception {
        final byte[] original = resource(name);
        final byte[] result = new LoggingMethodTransformer().transform(null, name, null, null, original);
        assertNotNull(result, name);
        final var errors = ClassFile.of().verify(result);
        if (name.equals("org/apache/logging/log4j/core/async/AsyncLogger")) {
            // Disruptor is optional and absent from this test runtime. Retained async methods
            // must produce exactly the same verifier diagnostics as the original class.
            final var originalErrors = ClassFile.of().verify(original);
            assertTrue(originalErrors.stream().allMatch(error -> error.getMessage()
                .contains("Could not resolve class EventTranslatorVararg")), () -> name + ": " + originalErrors);
            assertEquals(originalErrors.stream().map(Throwable::getMessage).toList(),
                errors.stream().map(Throwable::getMessage).toList(), name);
        } else {
            assertTrue(errors.isEmpty(), () -> name + ": " + errors);
        }
        return ClassFile.of().parse(result);
    }

    private static List<Instruction> instructions(final MethodModel method) {
        return method.code().orElseThrow().elementStream()
            .filter(Instruction.class::isInstance).map(Instruction.class::cast).toList();
    }

    private static List<String> shape(final ClassModel model) {
        return model.methods().stream().map(method -> method.methodName().stringValue()
            + method.methodType().stringValue() + "/" + method.flags().flagsMask()).toList();
    }

    @Test
    void actualLoggingEntriesBecomeStraightReturnsWithoutChangingClassShape() throws Exception {
        int noops = 0;
        int disabled = 0;
        for (final String name : List.of(ABSTRACT,
            "org/apache/logging/log4j/spi/ExtendedLoggerWrapper", "org/apache/logging/log4j/core/Logger",
            "org/apache/logging/log4j/core/async/AsyncLogger",
            "org/apache/logging/log4j/simple/SimpleLogger", "org/apache/logging/log4j/status/StatusLogger",
            "org/apache/logging/log4j/internal/DefaultLogBuilder", "org/apache/logging/log4j/EventLogger",
            "java/util/logging/Logger", "net/minecraft/launchwrapper/LogWrapper",
            "net/minecraftforge/fml/common/FMLLog", "net/minecraftforge/fml/relauncher/FMLRelaunchLog",
            "org/spongepowered/asm/logging/LoggerAdapterAbstract", "org/spongepowered/asm/logging/LoggerAdapterConsole",
            "org/spongepowered/asm/logging/LoggerAdapterJava", "org/spongepowered/asm/logging/LoggerAdapterDefault")) {
            final ClassModel original = ClassFile.of().parse(resource(name));
            final ClassModel transformed = transform(name);
            assertEquals(shape(original), shape(transformed), name);
            assertEquals(original.flags().flagsMask(), transformed.flags().flagsMask(), name);
            assertEquals(original.fields().stream().map(field -> field.fieldName().stringValue()
                + field.fieldType().stringValue() + "/" + field.flags().flagsMask()).toList(),
                transformed.fields().stream().map(field -> field.fieldName().stringValue()
                + field.fieldType().stringValue() + "/" + field.flags().flagsMask()).toList(), name);
            int targetEntries = 0;
            for (final MethodModel method : transformed.methods()) {
                final String methodName = method.methodName().stringValue();
                if (method.code().isEmpty()) continue;
                if (List.of("trace", "debug", "info", "warn", "error", "fatal", "log", "logMessage",
                    "logIfEnabled", "printf", "catching", "bigWarning", "logEvent", "severe", "warning",
                    "fine", "finer", "finest", "logp", "logrb").contains(methodName)
                    && method.methodTypeSymbol().returnType().descriptorString().equals("V")) {
                    assertEquals(List.of(Opcode.RETURN), instructions(method).stream().map(Instruction::opcode).toList(),
                        name + "." + methodName);
                    noops++;
                    targetEntries++;
                } else if (methodName.equals("isEnabled") || methodName.equals("isLoggable")
                    || methodName.startsWith("is") && methodName.endsWith("Enabled")) {
                    assertEquals(List.of(Opcode.ICONST_0, Opcode.IRETURN),
                        instructions(method).stream().map(Instruction::opcode).toList(), name + "." + methodName);
                    disabled++;
                    targetEntries++;
                }
            }
            assertTrue(targetEntries > 0, "Every existing fixed target must expose an intercepted entry: " + name);
            for (int index = 0; index < original.methods().size(); index++) {
                final MethodModel before = original.methods().get(index);
                if (!before.methodName().stringValue().startsWith("<")) continue;
                assertArrayEquals(((CodeAttribute) before.code().orElseThrow()).codeArray(),
                    ((CodeAttribute) transformed.methods().get(index).code().orElseThrow()).codeArray(), name);
            }
        }
        assertTrue(noops > 200, "Exercise the actual overload families rather than a synthetic logger");
        assertTrue(disabled > 40, "Every actual enabled-query overload must return false");
    }

    @Test
    void flowResultsStillReturnTheirOriginalArgumentsAndBuildersKeepTheirLifecycle() throws Exception {
        final ClassModel transformed = transform(ABSTRACT);
        int results = 0;
        for (final MethodModel method : transformed.methods()) {
            final String name = method.methodName().stringValue();
            if (!List.of("throwing", "exit", "traceExit").contains(name)
                || method.methodTypeSymbol().returnType().descriptorString().equals("V")) continue;
            final var code = instructions(method);
            assertEquals(2, code.size(), name);
            final LoadInstruction load = (LoadInstruction) code.getFirst();
            // All actual Log4j flow overloads are instance methods with reference arguments.
            assertEquals(method.methodTypeSymbol().parameterCount(), load.slot(), name);
            assertEquals(Opcode.ARETURN, code.getLast().opcode(), name);
            results++;
        }
        assertTrue(results >= 8);
        for (final MethodModel method : transformed.methods()) {
            if (!method.methodName().stringValue().startsWith("at")
                || !method.methodTypeSymbol().returnType().descriptorString()
                    .equals("Lorg/apache/logging/log4j/LogBuilder;")) continue;
            final var code = instructions(method);
            assertEquals(List.of(Opcode.GETSTATIC, Opcode.ARETURN), code.stream().map(Instruction::opcode).toList());
            final FieldInstruction field = (FieldInstruction) code.getFirst();
            assertEquals("NOOP", field.name().stringValue());
            assertEquals("org/apache/logging/log4j/LogBuilder", field.owner().asInternalName());
        }

        final String builderName = "org/apache/logging/log4j/internal/DefaultLogBuilder";
        final ClassModel before = ClassFile.of().parse(resource(builderName));
        final ClassModel after = transform(builderName);
        for (int index = 0; index < before.methods().size(); index++) {
            final MethodModel original = before.methods().get(index);
            final String name = original.methodName().stringValue();
            if (!name.equals("reset") && !name.startsWith("with")) continue;
            assertArrayEquals(((CodeAttribute) original.code().orElseThrow()).codeArray(),
                ((CodeAttribute) after.methods().get(index).code().orElseThrow()).codeArray(), name);
        }
    }

    @Test
    void unrelatedClassesAreRejectedBeforeParsing() {
        final LoggingMethodTransformer transformer = new LoggingMethodTransformer();
        assertNull(transformer.transform(null, "sample/OrdinaryClass", null, null, new byte[]{1, 2, 3}));
        assertNull(transformer.transform(null, null, null, null, new byte[]{1, 2, 3}));
        assertNull(transformer.getFailure());
    }

    @Test
    void firstTargetFailureRemainsVisibleAfterInstrumentationDiscardsTheException() {
        final LoggingMethodTransformer transformer = new LoggingMethodTransformer();
        final RuntimeException first = assertThrows(RuntimeException.class,
            () -> transformer.transform(null, ABSTRACT, null, null, new byte[]{1, 2, 3}));
        assertSame(first, transformer.getFailure());
        assertThrows(RuntimeException.class,
            () -> transformer.transform(null, ABSTRACT, null, null, new byte[]{4, 5, 6}));
        assertSame(first, transformer.getFailure());
    }
}
