package github.kasuminova.novaeng.common.performance;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.Constructor;
import org.spongepowered.asm.mixin.transformer.struct.InsnRange;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinFieldInitializationTest {

    @Test
    void registeredInstanceFieldInitializersAreNotDiscardedByConstructorLineRanges() throws Exception {
        final ClassLoader loader = this.getClass().getClassLoader();
        final JsonObject configuration;
        try (final var stream = loader.getResourceAsStream("novaeng_core.mixin.json")) {
            assertNotNull(stream);
            configuration = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        final String root = configuration.get("package").getAsString().replace('.', '/') + '/';
        final Set<String> inspected = new ObjectOpenHashSet<>();
        final List<String> missing = new ObjectArrayList<>();
        final List<String> unavailable = new ObjectArrayList<>();
        int compiledClasses = 0;
        int initializedFields = 0;
        for (final String group : new String[]{"mixins", "client", "server"}) {
            final JsonArray registrations = configuration.getAsJsonArray(group);
            if (registrations == null) {
                continue;
            }
            for (final var registration : registrations) {
                final String name = registration.getAsString();
                if (name.startsWith("enumcache.") || !inspected.add(name)) {
                    continue;
                }
                final ClassNode mixin = new ClassNode();
                try (final var stream = loader.getResourceAsStream(root + name.replace('.', '/') + ".class")) {
                    if (stream == null) {
                        unavailable.add(name);
                        continue;
                    }
                    new ClassReader(stream).accept(mixin, 0);
                    compiledClasses++;
                }
                final Set<String> fields = new ObjectOpenHashSet<>();
                for (final var field : mixin.fields) {
                    final boolean shadow = field.visibleAnnotations != null && field.visibleAnnotations.stream()
                                                                                                       .anyMatch(annotation -> annotation.desc.equals("Lorg/spongepowered/asm/mixin/Shadow;"));
                    if ((field.access & Opcodes.ACC_STATIC) == 0 && !shadow) {
                        fields.add(field.name);
                    }
                }
                for (final MethodNode constructor : mixin.methods) {
                    if (!constructor.name.equals("<init>")) {
                        continue;
                    }
                    final Set<String> assigned = new ObjectOpenHashSet<>();
                    for (final var instruction : constructor.instructions) {
                        if (instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTFIELD
                            && field.owner.equals(mixin.name) && fields.contains(field.name)) {
                            assigned.add(field.name);
                        }
                    }
                    if (assigned.isEmpty()) {
                        continue;
                    }
                    initializedFields += assigned.size();
                    final InsnRange range = Constructor.getRange(constructor);
                    final Set<String> extracted = new ObjectOpenHashSet<>();
                    if (range.isValid()) {
                        for (final var instruction : range.apply(constructor.instructions, false)) {
                            if (instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTFIELD
                                && field.owner.equals(mixin.name)) {
                                extracted.add(field.name);
                            }
                        }
                    }
                    assigned.removeAll(extracted);
                    for (final String field : assigned) {
                        missing.add(name + " " + constructor.desc + " field=" + field + " " + range);
                    }
                }
            }
        }
        System.out.println("Mixin constructor audit: " + inspected.size() + " registrations; " + compiledClasses
            + " compiled classes; " + initializedFields + " instance initializer assignments; unavailable="
            + unavailable + "; discarded=" + missing);
        assertTrue(compiledClasses > 0 && initializedFields > 0, "No compiled Mixin initializers were inspected");
        assertTrue(missing.isEmpty(), "Mixin will discard these field initializers: " + missing);
    }
}
