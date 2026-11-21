package github.kasuminova.novaeng.mixin.mmce;

import com.sun.management.ThreadMXBean;
import github.kasuminova.novaeng.common.performance.mmce.GenericItemHandlerCopy;
import hellfirepvp.modularmachinery.common.util.IItemHandlerImpl;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.util.Annotations;

import java.lang.management.ManagementFactory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericItemHandlerCopyTest {
    private static volatile Object allocationSink;

    @BeforeAll
    static void initializeVanillaItems() {
        Bootstrap.register();
    }

    private static GenericItemHandlerCopy implementation() throws Exception {
        final String original = MixinItemHandlerGenericCopy.class.getName().replace('.', '/');
        final String name = "github/kasuminova/novaeng/mixin/mmce/GenericCopyProbe";
        final ClassNode node = new ClassNode();
        try (final var input = GenericItemHandlerCopyTest.class.getResourceAsStream("MixinItemHandlerGenericCopy.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(new ClassRemapper(node, new SimpleRemapper(original, name)), 0);
        }
        node.access &= ~Opcodes.ACC_ABSTRACT;
        node.superName = IItemHandlerImpl.class.getName().replace('.', '/');
        // Use the real handler's fields and constructor; merge only the production implementation.
        node.fields.removeIf(field -> Annotations.getInvisible(field, Shadow.class) != null
            || Annotations.getVisible(field, Shadow.class) != null);
        for (final var method : node.methods) {
            if (method.name.equals("<init>")) for (final var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call && call.name.equals("<init>") && call.owner.equals("java/lang/Object"))
                    call.owner = node.superName;
        }
        final ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        final byte[] bytes = writer.toByteArray();
        final Class<?> type = new ClassLoader(GenericItemHandlerCopyTest.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(name.replace('/', '.'), bytes, 0, bytes.length);
            }
        }.define();
        return (GenericItemHandlerCopy) type.getConstructor().newInstance();
    }

    @Test
    void matchesActualGenericConstructorAndKeepsSourceStacksIndependent() throws Exception {
        final GenericItemHandlerCopy copy = implementation();
        final ItemStackHandler source = new ItemStackHandler(3);
        final ItemStack stack = new ItemStack(new Item(), 7);
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("variant", 1);
        stack.setTagCompound(tag);
        source.setStackInSlot(1, stack);
        final IItemHandlerImpl expected = new IItemHandlerImpl(source);
        final IItemHandlerImpl actual = copy.nova$copyFromGeneric(source);
        assertEquals(expected.getSlots(), actual.getSlots());
        for (int i = 0; i < source.getSlots(); i++) {
            assertEquals(expected.getStackInSlot(i).getCount(), actual.getStackInSlot(i).getCount());
            assertEquals(expected.getStackInSlot(i).getTagCompound(), actual.getStackInSlot(i).getTagCompound());
            assertEquals(expected.getSlotLimit(i), actual.getSlotLimit(i));
        }
        assertNotSame(stack, actual.getStackInSlot(1));
        tag.setInteger("variant", 2);
        assertEquals(1, actual.getStackInSlot(1).getTagCompound().getInteger("variant"));
        actual.getStackInSlot(1).shrink(2);
        assertEquals(7, source.getStackInSlot(1).getCount());
        copy.nova$releaseGenericContents();
        assertTrue(actual.getStackInSlot(1).isEmpty(), "Idle shells must release copied stacks and NBT");
    }

    @Test
    void reuseResetsSlotStructureAndDoesNotOverwriteSourceHandlerAliases() throws Exception {
        final GenericItemHandlerCopy copy = implementation();
        final IItemHandlerImpl actual = copy.nova$copyFromGeneric(new ItemStackHandler(8));
        final IItemHandlerImpl other = new IItemHandlerImpl(new int[]{2, 4}, new int[]{1, 5});
        final var in = IItemHandlerImpl.class.getDeclaredField("inSlots");
        in.setAccessible(true);
        final var out = IItemHandlerImpl.class.getDeclaredField("outSlots");
        out.setAccessible(true);
        // Reproduce native copyFrom's aliasing contract, including with the older compile API.
        in.set(actual, in.get(other));
        out.set(actual, out.get(other));
        copy.nova$copyFromGeneric(new ItemStackHandler(2));
        assertArrayEquals(new int[]{2, 4}, (int[]) in.get(other));
        assertArrayEquals(new int[]{1, 5}, (int[]) out.get(other));
        assertArrayEquals(new int[]{0, 1}, (int[]) in.get(actual));
        assertEquals(2, actual.getSlots());
        copy.nova$copyFromGeneric(new ItemStackHandler(500));
        copy.nova$releaseGenericContents();
        assertEquals(0, actual.getSlots(), "Oversized idle shell storage is discarded");
        assertEquals(0, copy.nova$copyFromGeneric(new ItemStackHandler(0)).getSlots());
    }

    @Test
    void emptyHandlerReuseAvoidsPerSimulationSlotShellAllocation() throws Exception {
        final GenericItemHandlerCopy copy = implementation();
        final ItemStackHandler source = new ItemStackHandler(32);
        for (int i = 0; i < 20000; i++) {
            allocationSink = new IItemHandlerImpl(source);
            allocationSink = copy.nova$copyFromGeneric(source);
            copy.nova$releaseGenericContents();
        }
        final var threads = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        final long thread = Thread.currentThread().threadId();
        final int count = 10000;
        final long before = threads.getThreadAllocatedBytes(thread);
        for (int i = 0; i < count; i++) allocationSink = new IItemHandlerImpl(source);
        final long baseline = threads.getThreadAllocatedBytes(thread) - before;
        final long reusedBefore = threads.getThreadAllocatedBytes(thread);
        for (int i = 0; i < count; i++) {
            allocationSink = copy.nova$copyFromGeneric(source);
            copy.nova$releaseGenericContents();
        }
        final long reused = threads.getThreadAllocatedBytes(thread) - reusedBefore;
        System.out.println("MMCE empty 32-slot handler bytes/op: original=" + baseline / (double) count
            + ", reused=" + reused / (double) count);
        assertTrue(baseline > 0);
        assertTrue(reused < baseline / 10, "Slot-shell reuse must remove structural allocation: " + reused + " / " + baseline);
    }

}
