package github.kasuminova.novaeng.common.performance;

import com.sun.management.ThreadMXBean;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TinkerTagQueriesTest {
    private static volatile int sink;

    private static Class<?> originalQueries() throws Exception {
        final ClassNode dependency = new ClassNode();
        try (final var input = TinkerTagQueriesTest.class.getResourceAsStream(
            "/slimeknights/tconstruct/library/utils/TinkerUtil.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(dependency, 0);
        }
        final ClassNode probe = new ClassNode();
        probe.version = dependency.version;
        probe.access = Opcodes.ACC_PUBLIC;
        probe.name = "github/kasuminova/novaeng/common/performance/OriginalTinkerQueries";
        probe.superName = "java/lang/Object";
        dependency.methods.stream().filter(method -> method.name.equals("hasTrait") || method.name.equals("hasModifier"))
                          .forEach(probe.methods::add);
        assertEquals(2, probe.methods.size(), "Check the actual supported TinkerUtil methods");
        final ClassWriter writer = new ClassWriter(0);
        probe.accept(writer);
        final byte[] bytes = writer.toByteArray();
        return new ClassLoader(TinkerTagQueriesTest.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(null, bytes, 0, bytes.length);
            }
        }.define();
    }

    @Test
    void matchesActualDependencyForMissingMalformedAndLiveTagData() throws Exception {
        final Class<?> original = originalQueries();
        final Method trait = original.getMethod("hasTrait", NBTTagCompound.class, String.class);
        final Method modifier = original.getMethod("hasModifier", NBTTagCompound.class, String.class);
        final NBTTagCompound tag = new NBTTagCompound();
        assertEquals(trait.invoke(null, null, "fluxed"), TinkerTagQueries.hasTrait(null, "fluxed"));
        assertEquals(modifier.invoke(null, null, "fluxed"), TinkerTagQueries.hasBaseModifier(null, "fluxed"));
        for (int mode = 0; mode < 6; mode++) {
            tag.removeTag("Traits");
            if (mode == 1) tag.setString("Traits", "wrong type");
            if (mode >= 2) {
                final NBTTagList list = new NBTTagList();
                if (mode == 3) list.appendTag(new NBTTagInt(1));
                if (mode >= 4) list.appendTag(new NBTTagString(mode == 4 ? "other" : "fluxed"));
                tag.setTag("Traits", list);
            }
            final NBTTagCompound base = new NBTTagCompound();
            if (tag.hasKey("Traits")) base.setTag("Modifiers", tag.getTag("Traits"));
            tag.setTag("TinkerData", base);
            assertEquals(trait.invoke(null, tag, "fluxed"), TinkerTagQueries.hasTrait(tag, "fluxed"));
            assertEquals(modifier.invoke(null, tag, "fluxed"), TinkerTagQueries.hasBaseModifier(tag, "fluxed"));
        }
        assertTrue(TinkerTagQueries.hasTrait(tag, "fluxed"));
        tag.removeTag("Traits");
        assertFalse(TinkerTagQueries.hasTrait(tag, "fluxed"), "Mutations must be observed without a stale cache");
        assertFalse(TinkerTagQueries.hasTrait(tag, null));
        final NBTTagList present = new NBTTagList();
        present.appendTag(new NBTTagString("fluxed"));
        tag.setTag("Traits", present);
        assertThrows(NullPointerException.class, () -> TinkerTagQueries.hasTrait(tag, null));
    }

    @Test
    void customCompoundReadsStillUseTheOriginalTagUtilBoundary() throws Exception {
        final NBTTagList traits = new NBTTagList();
        traits.appendTag(new NBTTagString("dynamic"));
        final NBTTagCompound custom = new NBTTagCompound() {
            @Override
            public NBTTagList getTagList(final String key, final int type) {
                return traits;
            }
        };
        final Method original = originalQueries().getMethod("hasTrait", NBTTagCompound.class, String.class);
        assertEquals(original.invoke(null, custom, "dynamic"), TinkerTagQueries.hasTrait(custom, "dynamic"));
        assertTrue(TinkerTagQueries.hasTrait(custom, "dynamic"));
    }

    @Test
    void warmedMissingQueriesDoNotAllocateNbtContainers() {
        final NBTTagCompound empty = new NBTTagCompound();
        final ThreadMXBean threads = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        final long thread = Thread.currentThread().threadId();
        for (int i = 0; i < 100000; i++) if (TinkerTagQueries.hasTrait(empty, "fluxed")) sink++;
        final long before = threads.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 100000; i++) {
            if (TinkerTagQueries.hasTrait(empty, "fluxed")) sink++;
            if (TinkerTagQueries.hasBaseModifier(empty, "fluxed")) sink++;
        }
        final long allocated = threads.getThreadAllocatedBytes(thread) - before;
        assertTrue(allocated < 16384, "Missing read-only queries allocated " + allocated + " bytes");
    }
}
