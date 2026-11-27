package github.kasuminova.novaeng.common.estorage;

import appeng.api.storage.ISaveProvider;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.storage.AbstractCellInventory;
import github.kasuminova.novaeng.mixin.ae2.AccessorAbstractCellInventory;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EStorageCellPersistenceTest {
    @Test
    void deferredSaveUsesPreviousSerializedCountAfterTheLiveCounterHasChanged() throws Exception {
        final CountingTag tag = new CountingTag();
        tag.setString("other-owner", "preserved");
        final Inventory inventory = Inventory.create(tag, 1_000_000);
        inventory.replace(10, 20, 30, 40, 50);
        inventory.persist();
        assertEquals(0, tag.removed, "first save must not scan the unused capacity");
        assertEquals(5, tag.getShort("it"));

        inventory.replace(70, 80);
        assertEquals(2, inventory.getStoredItemTypes(), "actual AbstractCellInventory.saveChanges updates the live counter before persist");
        assertEquals(5, tag.getShort("it"), "deferred save has not touched serialized metadata");
        tag.removed = 0;
        inventory.persist();
        assertEquals(6, tag.removed, "only the three obsolete stack/count slot pairs should be removed");
        for (int i = 0; i < 5; i++) {
            assertEquals(i < 2, tag.hasKey("#" + i));
            assertEquals(i < 2, tag.hasKey("@" + i));
        }
        assertEquals("preserved", tag.getString("other-owner"));
        assertEquals(150, tag.getLong("ic"));
        assertEquals(List.of(70L, 80L), Inventory.create(tag, 1_000_000).amounts());
    }

    @Test
    void repeatedGrowthShrinkEmptyAndReloadKeepExactlyTheSavedSlots() throws Exception {
        final CountingTag tag = new CountingTag();
        Inventory inventory = Inventory.create(tag, 1024);
        int previousCount = 0;
        for (final long[] amounts : new long[][]{{1, 2}, {3, 4, 5, 6}, {7, 8, 9, 10}, {11}, {}, {12, 13, 14}, {}}) {
            inventory.replace(amounts);
            tag.removed = 0;
            inventory.persist();
            final int slotRemovals = Math.max(0, previousCount - amounts.length) * 2;
            final int metadataRemovals = amounts.length == 0 ? 2 : 0;
            assertEquals(slotRemovals + metadataRemovals, tag.removed);
            assertEquals(amounts.length, tag.getShort("it"));
            long total = 0;
            final LongList expected = new LongArrayList(amounts.length);
            for (final long amount : amounts) { total += amount; expected.add(amount); }
            assertEquals(total, tag.getLong("ic"));
            for (int i = 0; i < Math.max(previousCount, amounts.length); i++) {
                assertEquals(i < amounts.length, tag.hasKey("#" + i));
                assertEquals(i < amounts.length, tag.hasKey("@" + i));
            }
            inventory = Inventory.create(tag, 1024);
            assertEquals(expected, inventory.amounts(), "actual EStorage loadCellItems must read back every saved amount");
            previousCount = amounts.length;
        }
    }

    private static final class CountingTag extends NBTTagCompound {
        int removed;
        @Override public void removeTag(final String key) { removed++; super.removeTag(key); }
    }

    /** Real subclass with Mixin-style return-type overloads that Java source cannot declare. */
    private static final class Inventory {
        private static final Class<?> TYPE = fixtureType();
        private final EStorageCellInventory<IAEItemStack> target;
        private final AccessorAbstractCellInventory access;

        private Inventory(final EStorageCellInventory<IAEItemStack> target) { this.target = target; access = (AccessorAbstractCellInventory) target; }

        @SuppressWarnings("unchecked")
        static Inventory create(final NBTTagCompound tag, final int capacity) throws Exception {
            final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            final EStorageCellInventory<IAEItemStack> target = (EStorageCellInventory<IAEItemStack>) ((Unsafe) unsafeField.get(null)).allocateInstance(TYPE);
            final Inventory inventory = new Inventory(target);
            write(target, "tagCompound", tag);
            write(target, "container", (ISaveProvider) ignored -> { });
            write(target, "maxItemTypes", capacity);
            write(target, "storedItemTypes", tag.getShort("it"));
            write(target, "storedItemCount", tag.getLong("ic"));
            write(target, "isPersisted", true);
            final Field channel = EStorageCellInventory.class.getDeclaredField("channel");
            channel.setAccessible(true);
            channel.set(target, Proxy.newProxyInstance(Inventory.class.getClassLoader(), new Class<?>[]{IStorageChannel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "createList" -> list(new ObjectArrayList<>());
                    case "createFromNBT" -> stack(((NBTTagCompound) args[0]).getString("name"), 1);
                    default -> throw new AssertionError("unexpected channel call " + method);
                }));
            return inventory;
        }

        void persist() { target.persist(); }
        short getStoredItemTypes() { return access.getStoredItemTypes(); }

        void replace(final long... amounts) throws Exception {
            final List<IAEItemStack> stacks = new ObjectArrayList<>(amounts.length);
            for (int i = 0; i < amounts.length; i++) stacks.add(stack("item-" + i, amounts[i]));
            write(target, "cellItems", list(stacks));
            final var save = AbstractCellInventory.class.getDeclaredMethod("saveChanges");
            save.setAccessible(true);
            save.invoke(target);
        }

        @SuppressWarnings("unchecked")
        LongList amounts() throws Exception {
            final var load = EStorageCellInventory.class.getDeclaredMethod("getCellItems");
            load.setAccessible(true);
            final IItemList<IAEItemStack> stacks = (IItemList<IAEItemStack>) load.invoke(target);
            final LongList amounts = new LongArrayList();
            for (final IAEItemStack stack : stacks) amounts.add(stack.getStackSize());
            return amounts;
        }

        private static Class<?> fixtureType() {
            final String name = "github/kasuminova/novaeng/common/estorage/AccessorCellFixture";
            final String test = Type.getInternalName(EStorageCellPersistenceTest.class);
            final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, Type.getInternalName(EStorageCellInventory.class), new String[]{Type.getInternalName(AccessorAbstractCellInventory.class)});
            for (final var method : AccessorAbstractCellInventory.class.getDeclaredMethods()) {
                final boolean getter = method.getParameterCount() == 0;
                final Class<?> valueClass = getter ? method.getReturnType() : method.getParameterTypes()[0];
                final Type value = Type.getType(valueClass);
                final String property = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
                final var bridge = writer.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
                bridge.visitCode(); bridge.visitVarInsn(Opcodes.ALOAD, 0); bridge.visitLdcInsn(property);
                final String wrapper = valueClass == short.class ? "java/lang/Short" : valueClass == int.class ? "java/lang/Integer" : valueClass == long.class ? "java/lang/Long" : valueClass == boolean.class ? "java/lang/Boolean" : null;
                if (getter) {
                    bridge.visitMethodInsn(Opcodes.INVOKESTATIC, test, "read", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", false);
                    bridge.visitTypeInsn(Opcodes.CHECKCAST, wrapper == null ? value.getInternalName() : wrapper);
                    if (wrapper != null) bridge.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapper, valueClass.getName() + "Value", "()" + value.getDescriptor(), false);
                    bridge.visitInsn(value.getOpcode(Opcodes.IRETURN));
                } else {
                    bridge.visitVarInsn(value.getOpcode(Opcodes.ILOAD), 1);
                    if (wrapper != null) bridge.visitMethodInsn(Opcodes.INVOKESTATIC, wrapper, "valueOf", "(" + value.getDescriptor() + ")L" + wrapper + ";", false);
                    bridge.visitMethodInsn(Opcodes.INVOKESTATIC, test, "write", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V", false);
                    bridge.visitInsn(Opcodes.RETURN);
                }
                bridge.visitMaxs(0,0); bridge.visitEnd();
            }
            writer.visitEnd();
            final byte[] bytes = writer.toByteArray();
            return new ClassLoader(Inventory.class.getClassLoader()) { Class<?> define() { return defineClass(name.replace('/', '.'), bytes, 0, bytes.length); } }.define();
        }
    }

    public static Object read(final Object target, final String name) {
        try { final Field field = AbstractCellInventory.class.getDeclaredField(name); field.setAccessible(true); return field.get(target); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    public static void write(final Object target, final String name, final Object value) {
        try { final Field field = AbstractCellInventory.class.getDeclaredField(name); field.setAccessible(true); field.set(target, value); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    @SuppressWarnings("unchecked")
    private static IItemList<IAEItemStack> list(final List<IAEItemStack> values) {
        return (IItemList<IAEItemStack>) Proxy.newProxyInstance(Inventory.class.getClassLoader(), new Class<?>[]{IItemList.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "size" -> values.size();
                case "isEmpty" -> values.isEmpty();
                case "iterator" -> values.iterator();
                case "add" -> { values.add((IAEItemStack) args[0]); yield null; }
                case "resetStatus" -> { values.clear(); yield null; }
                default -> throw new AssertionError("unexpected list call " + method);
            });
    }

    private static IAEItemStack stack(final String name, final long size) {
        final long[] amount = {size};
        return (IAEItemStack) Proxy.newProxyInstance(Inventory.class.getClassLoader(), new Class<?>[]{IAEItemStack.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getStackSize" -> amount[0];
                case "setStackSize" -> { amount[0] = (long) args[0]; yield proxy; }
                case "setCraftable" -> proxy;
                case "writeToNBT" -> { ((NBTTagCompound) args[0]).setString("name", name); yield null; }
                default -> throw new AssertionError("unexpected stack call " + method);
            });
    }
}
