package github.kasuminova.novaeng.client.font;

import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import github.kasuminova.novaeng.mixin.neofontrender.AccessorTooltipNbtCompound;
import github.kasuminova.novaeng.mixin.neofontrender.AccessorTooltipNbtList;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;

import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TooltipNbtTextTest {
    @Test
    void identityMatchesVanillaForNestedValuesEscapesAndAllTagTypes() {
        final NBTTagCompound root = new NBTTagCompound();
        root.setByte("byte", (byte) -12);
        root.setShort("short", (short) -32768);
        root.setInteger("int", Integer.MIN_VALUE);
        root.setLong("long", Long.MIN_VALUE);
        root.setFloat("float", Float.NaN);
        root.setDouble("double", Double.NEGATIVE_INFINITY);
        root.setByteArray("bytes", new byte[]{-128, 0, 127});
        root.setIntArray("ints", new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE});
        root.setTag("longs", new NBTTagLongArray(new long[]{Long.MIN_VALUE, Long.MAX_VALUE}));
        root.setTag("emptyList", new NBTTagList());
        root.setTag("emptyCompound", new NBTTagCompound());
        for (final String key : List.of("", "a b", "中文", "a\"b", "a\\b", "a\nb", "AZaz09._+-")) {
            root.setString(key, key + "\\\"\n\t😀");
        }
        final NBTTagList rows = new NBTTagList();
        final Random random = new Random(891734L);
        for (int i = 0; i < 100; i++) {
            final NBTTagCompound row = new NBTTagCompound();
            row.setString("name", "row " + i);
            row.setInteger("value", random.nextInt());
            row.setTag("nested", root.copy());
            rows.appendTag(row);
        }
        final NBTTagCompound bag = new NBTTagCompound();
        bag.setTag("items", rows);
        assertText(root);
        assertText(bag);
        // No caching by mutable object identity: deep edits are visible immediately.
        rows.getCompoundTagAt(0).getCompoundTag("nested").setString("changed", "new value");
        assertText(bag);
        assertEquals("prefix:null", TooltipNbtText.append(new StringBuilder("prefix:"), null).toString());
    }

    private static void assertText(final NBTBase value) {
        assertEquals("header_icon|" + value, TooltipNbtText.append(new StringBuilder("header_icon|"), value).toString());
    }

    @Test
    void subclassesRetainCustomTextAndFailuresDoNotAppendPartialIdentity() {
        final NBTTagCompound custom = new NBTTagCompound() {
            @Override public String toString() { return "custom-text"; }
        };
        assertEquals("custom-text", TooltipNbtText.append(new StringBuilder(), custom).toString());
        final NBTTagCompound root = new NBTTagCompound();
        root.setTag("failure", new NBTTagCompound() {
            @Override public String toString() { throw new IllegalStateException("custom failure"); }
        });
        final StringBuilder output = new StringBuilder("header_icon|");
        assertThrows(IllegalStateException.class, () -> TooltipNbtText.append(output, root));
        assertEquals("header_icon|", output.toString());
    }

    @Test
    void realInstalledTooltipMethodRedirectsOnlyTheCompoundAppend() throws Exception {
        final byte[] transformed = IsolatedMixinTransformer.transformInPackage(
            "github.kasuminova.novaeng.mixin.neofontrender",
            new String[]{"neofontrender.addons.tooltips.TooltipPreviewRenderers"}, "MixinTooltipIdentityText")[0];
        final ClassNode node = new ClassNode();
        new ClassReader(transformed).accept(node, 0);
        final var method = node.methods.stream().filter(m -> m.name.equals("appendStackIdentity")).findFirst().orElseThrow();
        int redirects = 0;
        int objectAppends = 0;
        for (final var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.name.contains("appendNbtWithoutSubtreeStrings")) redirects++;
                if (call.owner.equals("java/lang/StringBuilder") && call.name.equals("append")
                    && call.desc.startsWith("(Ljava/lang/Object;)")) objectAppends++;
            }
        }
        assertEquals(1, redirects);
        assertEquals(1, objectAppends, "registry-name exception fallback must remain untouched");
    }

    @Test
    void backingAccessorsReadTheFieldsWithoutCallingStellarMutableAccessors() throws Exception {
        final byte[][] classes = IsolatedMixinTransformer.transformInPackage(
            "github.kasuminova.novaeng.mixin.neofontrender",
            new String[]{"net.minecraft.nbt.NBTTagCompound", "net.minecraft.nbt.NBTTagList"},
            "AccessorTooltipNbtCompound", "AccessorTooltipNbtList");
        for (int i = 0; i < classes.length; i++) {
            final ClassNode node = new ClassNode();
            new ClassReader(classes[i]).accept(node, 0);
            final String expected = i == 0 ? "nova$tooltipTagMap" : "nova$tooltipTagList";
            final var method = node.methods.stream().filter(m -> m.name.equals(expected)).findFirst().orElseThrow();
            int fields = 0;
            for (final var instruction : method.instructions) {
                assertFalse(instruction instanceof MethodInsnNode);
                if (instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.GETFIELD) fields++;
            }
            assertEquals(1, fields);
        }
    }

    @Test
    void realStreamingPathReadsTransformedBackingFieldsAndNeverMutableGetters() throws Exception {
        final String compoundName = "net.minecraft.nbt.NBTTagCompound";
        final String listName = "net.minecraft.nbt.NBTTagList";
        final String helperName = TooltipNbtText.class.getName();
        final byte[][] transformed = IsolatedMixinTransformer.transformInPackage(
            "github.kasuminova.novaeng.mixin.neofontrender", new String[]{compoundName, listName},
            "AccessorTooltipNbtCompound", "AccessorTooltipNbtList");
        final Map<String, byte[]> definitions = new HashMap<>();
        for (int i = 0; i < transformed.length; i++) {
            final ClassNode node = new ClassNode();
            new ClassReader(transformed[i]).accept(node, 0);
            for (final var method : node.methods) {
                // StellarCore gives these getters mutation side effects. Throwing here proves
                // streaming traverses the fields instead. Vanilla toString never calls them.
                if (method.name.equals(i == 0 ? "getTag" : "get")) {
                    method.instructions.clear();
                    method.tryCatchBlocks.clear();
                    if (method.localVariables != null) method.localVariables.clear();
                    method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                    method.instructions.add(new InsnNode(Opcodes.ATHROW));
                }
            }
            final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            definitions.put(i == 0 ? compoundName : listName, writer.toByteArray());
        }
        try (final var input = getClass().getClassLoader().getResourceAsStream(helperName.replace('.', '/') + ".class")) {
            assertNotNull(input);
            definitions.put(helperName, input.readAllBytes());
        }
        final ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
            @Override protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                // NBTBase exposes protected/package-private operations used by its concrete
                // tags. Keep the entire runtime package in this loader, not just containers.
                if (!definitions.containsKey(name) && name.startsWith("net.minecraft.nbt.")) {
                    try (final var input = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        if (input == null) throw new ClassNotFoundException(name);
                        definitions.put(name, input.readAllBytes());
                    } catch (final java.io.IOException failure) {
                        throw new ClassNotFoundException(name, failure);
                    }
                }
                if (!definitions.containsKey(name)) return super.loadClass(name, resolve);
                Class<?> type = findLoadedClass(name);
                if (type == null) {
                    final byte[] bytes = definitions.get(name);
                    type = defineClass(name, bytes, 0, bytes.length);
                }
                if (resolve) resolveClass(type);
                return type;
            }
        };
        final Class<?> compound = loader.loadClass(compoundName);
        final Class<?> list = loader.loadClass(listName);
        final Object root = compound.getConstructor().newInstance();
        final Object rows = list.getConstructor().newInstance();
        // Accessor return types erase to Map/List; child tags intentionally use their own
        // NBTBase runtime package, so the test handles values as Objects across the boundary.
        @SuppressWarnings({"rawtypes", "unchecked"})
        final Map<String, Object> tags = (Map) ((AccessorTooltipNbtCompound) root).nova$tooltipTagMap();
        tags.put("nested list", rows);
        @SuppressWarnings({"rawtypes", "unchecked"})
        final List<Object> entries = (List) ((AccessorTooltipNbtList) rows).nova$tooltipTagList();
        final var stringTag = loader.loadClass("net.minecraft.nbt.NBTTagString").getConstructor(String.class);
        final var intTag = loader.loadClass("net.minecraft.nbt.NBTTagInt").getConstructor(int.class);
        final var byteArrayTag = loader.loadClass("net.minecraft.nbt.NBTTagByteArray").getConstructor(byte[].class);
        for (int i = 0; i < 32; i++) {
            final Object row = compound.getConstructor().newInstance();
            @SuppressWarnings({"rawtypes", "unchecked"})
            final Map<String, Object> fields = (Map) ((AccessorTooltipNbtCompound) row).nova$tooltipTagMap();
            fields.put("quote\"slash\\unicode😀", stringTag.newInstance("string \"\\\n中文 " + i));
            fields.put("number", intTag.newInstance(i));
            fields.put("array", byteArrayTag.newInstance(new byte[]{-128, 0, 127}));
            entries.add(row);
        }
        final var append = loader.loadClass(helperName).getMethod("append", StringBuilder.class, Object.class);
        assertEquals("header_icon|" + root, append.invoke(null, new StringBuilder("header_icon|"), root).toString());
        final Object row = entries.getFirst();
        @SuppressWarnings({"rawtypes", "unchecked"})
        final Map<String, Object> fields = (Map) ((AccessorTooltipNbtCompound) row).nova$tooltipTagMap();
        fields.put("changed", stringTag.newInstance("new"));
        assertEquals("header_icon|" + root, append.invoke(null, new StringBuilder("header_icon|"), root).toString());
    }
}
