package github.kasuminova.novaeng.common.performance;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IC2ConfigPathsTest {

    static ClassLoader transformedConfigLoader() {
        return new ClassLoader(IC2ConfigPathsTest.class.getClassLoader()) {
            @Override
            protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                if (!name.equals("ic2.core.util.Config") && !name.startsWith("ic2.core.util.Config$")) {
                    return super.loadClass(name, resolve);
                }
                Class<?> loaded = this.findLoadedClass(name);
                if (loaded == null) {
                    try {
                        byte[] bytes = resource(name.replace('.', '/') + ".class");
                        if (name.equals("ic2.core.util.Config")) {
                            bytes = redirectConfigRead(bytes);
                        }
                        loaded = this.defineClass(name, bytes, 0, bytes.length);
                    } catch (final IOException exception) {
                        throw new ClassNotFoundException(name, exception);
                    }
                }
                if (resolve) {
                    this.resolveClass(loaded);
                }
                return loaded;
            }
        };
    }

    private static byte[] redirectConfigRead(final byte[] original) throws IOException {
        final String target = "ic2/core/util/Config";
        final String mixinName = "github/kasuminova/novaeng/mixin/ic2/MixinConfigTickPath";
        final ClassNode config = new ClassNode();
        new ClassReader(original).accept(config, 0);
        final ClassNode mixin = new ClassNode();
        new ClassReader(resource(mixinName + ".class")).accept(mixin, 0);
        final MethodNode redirect = mixin.methods.stream().filter(method -> method.name.equals("nova$splitTickPath"))
                                                 .findFirst().orElseThrow();
        for (final AbstractInsnNode instruction : redirect.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(mixinName)) {
                call.owner = target;
            }
        }
        config.methods.add(redirect);
        int replacements = 0;
        for (final MethodNode method : config.methods) {
            if (!method.name.equals("get") || !method.desc.equals("(Ljava/lang/String;)Lic2/core/util/Config$Value;")) {
                continue;
            }
            for (final AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals(target) && call.name.equals("split")) {
                    call.name = redirect.name;
                    replacements++;
                }
            }
        }
        assertEquals(1, replacements);
        final ClassWriter writer = new ClassWriter(0);
        config.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] resource(final String name) throws IOException {
        try (final InputStream stream = IC2ConfigPathsTest.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                throw new IOException("Missing test resource: " + name);
            }
            return stream.readAllBytes();
        }
    }

    @Test
    void sharedPathIsImmutableAndOnlyMatchesTheExactReadPath() {
        final List<String> path = IC2ConfigPaths.tickPath("balance/disableEnderChest", '/');
        assertEquals(List.of("balance", "disableEnderChest"), path);
        assertSame(path, IC2ConfigPaths.tickPath("balance/disableEnderChest", '/'));
        assertThrows(UnsupportedOperationException.class, () -> path.set(0, "other"));
        assertNull(IC2ConfigPaths.tickPath("balance/disableEnderChest", '='));
        assertNull(IC2ConfigPaths.tickPath("balance/disableEnderChest/extra", '/'));
        assertNull(IC2ConfigPaths.tickPath(null, '/'));
    }

    @Test
    void actualRedirectBodyRetainsDynamicValuesClearReloadAndFallback() throws Exception {
        final ClassLoader loader = transformedConfigLoader();
        final Class<?> configClass = loader.loadClass("ic2.core.util.Config");
        final Object config = configClass.getConstructor(String.class).newInstance("test");
        final Method set = configClass.getMethod("set", String.class, Object.class);
        final Method get = configClass.getMethod("get", String.class);
        set.invoke(config, "balance/disableEnderChest", true);
        final Object originalValue = get.invoke(config, "balance/disableEnderChest");
        final Method getBool = originalValue.getClass().getMethod("getBool");
        assertEquals(true, getBool.invoke(originalValue));
        set.invoke(config, "balance/disableEnderChest", false);
        assertSame(originalValue, get.invoke(config, "balance/disableEnderChest"));
        assertEquals(false, getBool.invoke(get.invoke(config, "balance/disableEnderChest")));
        set.invoke(config, "general/enabled", true);
        assertEquals(true, getBool.invoke(get.invoke(config, "general/enabled")));
        configClass.getMethod("remove", String.class).invoke(config, "balance/disableEnderChest");
        assertNull(get.invoke(config, "balance/disableEnderChest"));
        configClass.getMethod("clear").invoke(config);
        assertNull(get.invoke(config, "general/enabled"));
        final byte[] replacement = "[balance]\ndisableEnderChest = true\n".getBytes(StandardCharsets.UTF_8);
        configClass.getMethod("load", InputStream.class).invoke(config, new ByteArrayInputStream(replacement));
        assertEquals(true, getBool.invoke(get.invoke(config, "balance/disableEnderChest")));
        assertNotSame(originalValue, get.invoke(config, "balance/disableEnderChest"));
    }
}
