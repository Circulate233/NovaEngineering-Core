package github.kasuminova.novaeng.mixin.mmce;

import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActiveRecipeLazyDataTest {
    private static final String TARGET = "hellfirepvp/modularmachinery/common/crafting/ActiveMachineRecipe";
    private static final String TAG = "net/minecraft/nbt/NBTTagCompound";

    @BeforeAll
    static void initializeMinecraft() {
        Bootstrap.register();
    }

    private static ClassNode read(final String name) throws Exception {
        try (final var input = ActiveRecipeLazyDataTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, name);
            final ClassNode result = new ClassNode();
            new ClassReader(input).accept(result, 0);
            return result;
        }
    }

    private static Class<?> transformedRecipe() throws Exception {
        final String name = "github/kasuminova/novaeng/mixin/mmce/LazyRecipeProbe";
        final byte[] transformed = github.kasuminova.novaeng.testutil.IsolatedMixinTransformer.transform(
            new String[]{TARGET}, "MixinActiveRecipeLazyData")[0];
        final ClassNode target = new ClassNode();
        new ClassReader(transformed).accept(new ClassRemapper(target, new SimpleRemapper(TARGET, name)), 0);
        int deferred = 0;
        for (final MethodNode method : target.methods) {
            if (!method.name.equals("<init>") && !method.name.equals("reset")) continue;
            for (final AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW) {
                    assertFalse(type.desc.equals(TAG), "Untouched recipe construction/reset must allocate no data tag");
                    assertFalse(type.desc.equals("java/lang/NullPointerException"), "No constructor redirect null guard");
                }
                if (instruction instanceof MethodInsnNode call && call.name.contains("deferUnusedData")) deferred++;
            }
        }
        assertEquals(3, deferred, "Both constructors and reset must defer exactly one data compound each");
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        target.accept(writer);
        final byte[] bytes = writer.toByteArray();
        return new ClassLoader(ActiveRecipeLazyDataTest.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(null, bytes, 0, bytes.length);
            }
        }.define();
    }

    @Test
    void actualDependencyHasNoUnaccountedDirectDataReaders() throws Exception {
        final var allowed = Set.of("<init>", "reset", "getData", "getDataCompound", "setData", "serialize");
        for (final MethodNode method : read(TARGET).methods) {
            for (final AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof FieldInsnNode field && field.owner.equals(TARGET) && field.name.equals("data"))
                    assertTrue(allowed.contains(method.name), "Review data access in " + method.name);
            }
        }
    }

    @Test
    void unusedDataStaysUnallocatedAndWritableDataSurvivesSerializationAndReset() throws Exception {
        final Class<?> type = transformedRecipe();
        final MachineRecipe recipe = new MachineRecipe("test", new ResourceLocation("novaeng", "allocation_test"),
            new ResourceLocation("novaeng", "machine"), 20, 0, false, false);
        final Object first = type.getConstructor(MachineRecipe.class, int.class).newInstance(recipe, 4);
        final Object second = type.getConstructor(MachineRecipe.class, int.class).newInstance(recipe, 4);
        final var field = type.getDeclaredField("data");
        field.setAccessible(true);
        final var getter = type.getMethod("getDataCompound");
        assertNull(field.get(first));
        assertFalse(((NBTTagCompound) type.getMethod("serialize").invoke(first)).hasKey("data"));
        assertNull(field.get(first), "Saving an untouched recipe must not initialize its data");
        final NBTTagCompound data = (NBTTagCompound) getter.invoke(first);
        assertSame(data, getter.invoke(first));
        assertNotSame(data, getter.invoke(second));
        data.setInteger("progress", 42);
        assertNotNull(type.getMethod("getData").invoke(first));
        final NBTTagCompound serialized = (NBTTagCompound) type.getMethod("serialize").invoke(first);
        assertEquals(42, serialized.getCompoundTag("data").getInteger("progress"));
        type.getMethod("reset").invoke(first);
        assertNull(field.get(first));
        final NBTTagCompound reset = (NBTTagCompound) getter.invoke(first);
        assertTrue(reset.isEmpty());
        assertNotSame(data, reset);
        assertEquals(42, data.getInteger("progress"), "Reset must not mutate previously exposed writable data");
        final Object restored = type.getConstructor(NBTTagCompound.class).newInstance(serialized);
        assertSame(serialized.getCompoundTag("data"), getter.invoke(restored));
        final NBTTagCompound withoutData = serialized.copy();
        withoutData.removeTag("data");
        final Object restoredEmpty = type.getConstructor(NBTTagCompound.class).newInstance(withoutData);
        assertNull(field.get(restoredEmpty), "Deserializing without custom data must also stay lazy");
        type.getMethod("reset").invoke(restoredEmpty);
        assertNull(field.get(restoredEmpty), "Resetting untouched data must remain allocation free");
    }
}
