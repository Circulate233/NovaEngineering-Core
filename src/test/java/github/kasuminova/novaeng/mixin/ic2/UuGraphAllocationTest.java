package github.kasuminova.novaeng.mixin.ic2;

import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UuGraphAllocationTest {
    private static NBTTagCompound tag(final int variant) {
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("variant", variant);
        return tag;
    }

    static ClassNode readClass(final ClassLoader loader, final String name) throws Exception {
        final var resource = loader.getResource(name + ".class");
        assertNotNull(resource, "actual dependency bytecode: " + name);
        // Gradle's access-transformed IC2 JAR can retain upstream signature metadata.
        // Read its actual transformed bytes without asking URLClassLoader to verify the old digest.
        if (resource.openConnection() instanceof JarURLConnection jar) {
            try (final ZipFile zip = new ZipFile(Path.of(jar.getJarFileURL().toURI()).toFile());
                 final var in = zip.getInputStream(zip.getEntry(jar.getEntryName()))) {
                final ClassNode node = new ClassNode();
                new ClassReader(in.readAllBytes()).accept(node, 0);
                return node;
            }
        }
        try (final var in = resource.openStream()) {
            final ClassNode node = new ClassNode();
            new ClassReader(in.readAllBytes()).accept(node, 0);
            return node;
        }
    }

    /**
     * Transplant the actual overwrite and initializer into upstream IC2, retaining getInternal.
     */
    private static void applyMixin(final ClassNode original, final ClassLoader loader) throws Exception {
        final ClassNode mixin = readClass(loader, "github/kasuminova/novaeng/mixin/ic2/MixinUuGraph");
        final MethodNode internal = original.methods.stream().filter(m -> m.name.equals("getInternal")).findFirst().orElseThrow();
        for (final var field : original.fields) {
            if (field.name.equals("nodes") || field.name.equals("itemNodes")) field.access &= ~Opcodes.ACC_FINAL;
        }
        boolean overwrite = false;
        boolean initializer = false;
        for (final MethodNode method : mixin.methods) {
            // If the old HEAD injector returns, weave it into getInternal as well. Merely copying
            // getAll and invoking the injector by hand would miss the production call sequence.
            final boolean queryHead = annotations(method).stream().anyMatch(annotation ->
                annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")
                    && ((List<?>) value(annotation, "method")).contains("getInternal"));
            if (!method.name.equals("getAll") && !method.name.equals("onInit") && !queryHead) continue;
            if (queryHead) {
                final AnnotationNode inject = annotations(method).stream().filter(a -> a.desc.endsWith("/Inject;")).findFirst().orElseThrow();
                final AnnotationNode at = (AnnotationNode) ((List<?>) value(inject, "at")).getFirst();
                assertEquals("HEAD", value(at, "value"), "extend the harness for a new injection point");
                assertEquals(Boolean.TRUE, value(inject, "cancellable"));
                weaveQueryHead(original, internal, method);
            }
            method.visibleAnnotations = null;
            method.invisibleAnnotations = null;
            for (final var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals(mixin.name))
                    call.owner = original.name;
                if (instruction instanceof FieldInsnNode field && field.owner.equals(mixin.name))
                    field.owner = original.name;
            }
            if (method.name.equals("getAll")) {
                original.methods.removeIf(m -> m.name.equals("getAll"));
                overwrite = true;
            }
            if (method.name.equals("onInit")) {
                final MethodNode clinit = original.methods.stream().filter(m -> m.name.equals("<clinit>")).findFirst().orElseThrow();
                for (final var instruction : clinit.instructions.toArray()) {
                    if (instruction.getOpcode() != Opcodes.RETURN) continue;
                    final InsnList tail = new InsnList();
                    tail.add(new TypeInsnNode(Opcodes.NEW, "org/spongepowered/asm/mixin/injection/callback/CallbackInfo"));
                    tail.add(new InsnNode(Opcodes.DUP));
                    tail.add(new LdcInsnNode("<clinit>"));
                    tail.add(new InsnNode(Opcodes.ICONST_0));
                    tail.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "org/spongepowered/asm/mixin/injection/callback/CallbackInfo", "<init>", "(Ljava/lang/String;Z)V", false));
                    tail.add(new MethodInsnNode(Opcodes.INVOKESTATIC, original.name, method.name, method.desc, false));
                    clinit.instructions.insertBefore(instruction, tail);
                }
                initializer = true;
            }
            original.methods.add(method);
        }
        assertTrue(overwrite && initializer, "apply both the actual getAll overwrite and map initialization");
    }

    private static List<AnnotationNode> annotations(final MethodNode method) {
        return method.visibleAnnotations == null ? ObjectLists.emptyList() : method.visibleAnnotations;
    }

    private static Object value(final AnnotationNode annotation, final String key) {
        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (annotation.values.get(i).equals(key)) return annotation.values.get(i + 1);
        }
        return null;
    }

    private static void weaveQueryHead(final ClassNode graph, final MethodNode internal, final MethodNode hook) {
        final String callback = "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
        final int local = internal.maxLocals++;
        final InsnList head = new InsnList();
        head.add(new TypeInsnNode(Opcodes.NEW, callback));
        head.add(new InsnNode(Opcodes.DUP));
        head.add(new LdcInsnNode("getInternal"));
        head.add(new InsnNode(Opcodes.ICONST_1));
        head.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, callback, "<init>", "(Ljava/lang/String;Z)V", false));
        head.add(new VarInsnNode(Opcodes.ASTORE, local));
        head.add(new VarInsnNode(Opcodes.ALOAD, 0));
        head.add(new VarInsnNode(Opcodes.ALOAD, local));
        head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, graph.name, hook.name, hook.desc, false));
        head.add(new VarInsnNode(Opcodes.ALOAD, local));
        head.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, callback, "isCancelled", "()Z", false));
        final LabelNode proceed = new LabelNode();
        head.add(new JumpInsnNode(Opcodes.IFEQ, proceed));
        head.add(new VarInsnNode(Opcodes.ALOAD, local));
        head.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, callback, "getReturnValue", "()Ljava/lang/Object;", false));
        head.add(new TypeInsnNode(Opcodes.CHECKCAST, graph.name + "$Node"));
        head.add(new InsnNode(Opcodes.ARETURN));
        head.add(proceed);
        // At the original method entry only its LeanItemStack argument remains live.
        head.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        internal.instructions.insert(head);
    }

    @Test
    void exactLookupKeepsNormalizedNodesAndWildcardViewsLive() throws Exception {
        for (final boolean optimized : new boolean[]{false, true}) {
            final Graph graph = new Graph(optimized);
            final Item item = new Item();
            final Object stack = graph.stack(item, 4, null, 64);
            final Object node = graph.lookup(stack);
            final Field nodeStack = node.getClass().getDeclaredField("stack");
            nodeStack.setAccessible(true);
            assertNotSame(stack, nodeStack.get(node));
            assertEquals(1, graph.lean.getMethod("getSize").invoke(nodeStack.get(node)));
            assertEquals(64, graph.lean.getMethod("getSize").invoke(stack));
            assertSame(node, graph.lookup(stack));
            assertSame(node, graph.lookup(graph.stack(item, 4, null, 8)));
            assertEquals(0, graph.cachedHash.getInt(stack));

            final Collection<?> view = graph.all(graph.stack(item, 32767, null, 1));
            assertEquals(1, view.size());
            final Object taggedNode = graph.lookup(graph.stack(item, 4, tag(1), 8));
            assertNotSame(node, taggedNode);
            assertEquals(2, view.size(), "wildcard results must remain live and include NBT variants");
            assertNotSame(node, graph.lookup(graph.stack(item, 9, null, 1)));
            assertEquals(3, view.size(), "misses must still maintain IC2's item index");
            assertTrue(graph.all(graph.stack(new Item(), 32767, null, 1)).isEmpty());
        }
    }

    @Test
    void reusingTheQueryAfterNbtMutationFindsTheExistingNodeWithoutCachingItsHash() throws Exception {
        for (final boolean optimized : new boolean[]{false, true}) {
            final Graph graph = new Graph(optimized);
            final Item item = new Item();
            // Seed independent keys so mutating the query does not also mutate a stored key.
            final Object first = graph.lookup(graph.stack(item, 4, tag(1), 1));
            final Object second = graph.lookup(graph.stack(item, 4, tag(2), 1));
            final NBTTagCompound mutableTag = tag(1);
            final Object query = graph.stack(item, 4, mutableTag, 64);
            final Method copy = graph.lean.getMethod("copyWithSize", int.class);
            final int firstHash = copy.invoke(query, 1).hashCode();
            assertSame(first, graph.lookup(query));
            final int hashAfterFirstQuery = graph.cachedHash.getInt(query);

            mutableTag.setInteger("variant", 2);
            assertNotEquals(firstHash, graph.stack(item, 4, tag(2), 1).hashCode(), "fixture must change the query hash");
            assertSame(second, graph.lookup(query), "reuse after NBT mutation must find the existing second node");
            assertEquals(0, hashAfterFirstQuery, "a hit must not cache the caller's hash");
            assertSame(second, graph.lookup(query));
            assertEquals(0, graph.cachedHash.getInt(query));
            assertEquals(2, graph.nodes.size(), "query reuse must not insert a duplicate equal node");
            assertEquals(2, graph.all(graph.stack(item, 32767, null, 1)).size());

            // Exercise insertion too, then reuse that caller to find an existing key.
            final NBTTagCompound missTag = tag(3);
            final Object miss = graph.stack(item, 4, missTag, 32);
            assertNotSame(second, graph.lookup(miss));
            assertEquals(0, graph.cachedHash.getInt(miss), "a miss must hash the normalized copy too");
            missTag.setInteger("variant", 2);
            assertSame(second, graph.lookup(miss));
            assertEquals(0, graph.cachedHash.getInt(miss));
            assertEquals(3, graph.nodes.size());
        }
    }

    private static final class Graph {
        private final Class<?> lean;
        private final Constructor<?> constructor;
        private final Method all;
        private final Field cachedHash;
        private final Map<?, ?> nodes;

        private Graph(final boolean optimized) throws Exception {
            final ClassLoader loader = new ClassLoader(UuGraphAllocationTest.class.getClassLoader()) {
                @Override
                protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                    if (!name.startsWith("ic2.core.uu.")) return super.loadClass(name, resolve);
                    Class<?> type = findLoadedClass(name);
                    if (type == null) {
                        try {
                            final ClassNode original = readClass(getParent(), name.replace('.', '/'));
                            if (optimized && name.equals("ic2.core.uu.UuGraph")) applyMixin(original, getParent());
                            final ClassWriter out = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                            original.accept(out);
                            final byte[] bytes = out.toByteArray();
                            type = defineClass(name, bytes, 0, bytes.length);
                        } catch (Exception failure) {
                            throw new ClassNotFoundException(name, failure);
                        }
                    }
                    if (resolve) resolveClass(type);
                    return type;
                }
            };
            lean = loader.loadClass("ic2.core.uu.LeanItemStack");
            final Class<?> graph = loader.loadClass("ic2.core.uu.UuGraph");
            constructor = lean.getDeclaredConstructor(Item.class, int.class, NBTTagCompound.class, int.class);
            constructor.setAccessible(true);
            all = graph.getDeclaredMethod("getAll", lean);
            all.setAccessible(true);
            cachedHash = lean.getDeclaredField("hashCode");
            cachedHash.setAccessible(true);
            final Field nodeMap = graph.getDeclaredField("nodes");
            nodeMap.setAccessible(true);
            nodes = (Map<?, ?>) nodeMap.get(null);
            if (optimized)
                assertEquals("it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap", nodes.getClass().getName());
        }

        private Object stack(final Item item, final int meta, final NBTTagCompound tag, final int size) throws Exception {
            return constructor.newInstance(item, meta, tag, size);
        }

        private Collection<?> all(final Object stack) throws Exception {
            return (Collection<?>) all.invoke(null, stack);
        }

        private Object lookup(final Object stack) throws Exception {
            return all(stack).iterator().next();
        }
    }
}
