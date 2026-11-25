package github.kasuminova.novaeng.common.assembly;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.UnaryOperator;

public final class AssemblyRecovery extends WorldSavedData {
    private static final Object2ObjectOpenHashMap<UUID, AssemblyRecovery> PENDING = new Object2ObjectOpenHashMap<>();
    private final ObjectArrayList<ItemStack> stacks = new ObjectArrayList<>();

    public AssemblyRecovery(String name) {
        super(name);
    }

    static void load(EntityPlayerMP player) {
        var uuid = player.getUniqueID();
        var saved = find(storage(player.getServerWorld()), name(uuid));
        if (saved != null && !saved.stacks.isEmpty()) PENDING.put(uuid, saved);
    }

    private static MapStorage storage(WorldServer world) {
        var server = Objects.requireNonNull(world.getMinecraftServer());
        return server.getWorld(0).getMapStorage();
    }

    private static String name(UUID player) {
        return "novaeng_assembly_recovery_" + player;
    }

    private static @Nullable AssemblyRecovery find(MapStorage storage, String name) {
        return (AssemblyRecovery) storage.getOrLoadData(AssemblyRecovery.class, name);
    }

    static void keep(WorldServer world, UUID player, ItemStack stack) {
        var data = PENDING.get(player);
        if (data == null) {
            var storage = storage(world);
            var name = name(player);
            data = find(storage, name);
            if (data == null) {
                data = new AssemblyRecovery(name);
                storage.setData(name, data);
            }
            PENDING.put(player, data);
        }
        data.retain(stack);
    }

    static boolean pending(UUID player) {
        return PENDING.containsKey(player);
    }

    static Result deliver(EntityPlayerMP player, @Nullable AssemblyMaterials ae, BooleanSupplier available) {
        var uuid = player.getUniqueID();
        var data = PENDING.get(uuid);
        if (data == null) return Result.EMPTY;
        var result = data.drain(available, stack -> ae == null ? AssemblyMaterials.recoverInventory(player, stack) : ae.recover(stack));
        if (result == Result.EMPTY) PENDING.remove(uuid);
        return result;
    }

    void retain(ItemStack stack) {
        stacks.add(stack);
        markDirty();
    }

    Result drain(BooleanSupplier available, UnaryOperator<ItemStack> insert) {
        int resolved = 0;
        try {
            while (resolved < stacks.size() && available.getAsBoolean()) {
                var stack = stacks.get(resolved);
                int count = stack.getCount();
                var remaining = insert.apply(stack);
                if (!remaining.isEmpty()) {
                    if (remaining.getCount() != count || remaining != stack && !ItemStack.areItemStacksEqual(stack, remaining)) {
                        stacks.set(resolved, remaining);
                        markDirty();
                    }
                    return Result.FULL;
                }
                resolved++;
            }
        } finally {
            if (resolved != 0) {
                stacks.removeElements(0, resolved);
                markDirty();
            }
        }
        return stacks.isEmpty() ? Result.EMPTY : Result.PENDING;
    }

    static void stop() {
        PENDING.clear();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        stacks.clear();
        var list = tag.getTagList("Stacks", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) stacks.add(new ItemStack(list.getCompoundTagAt(i)));
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(@NotNull NBTTagCompound tag) {
        var list = new NBTTagList();
        for (int i = 0; i < stacks.size(); i++) list.appendTag(stacks.get(i).serializeNBT());
        tag.setTag("Stacks", list);
        return tag;
    }

    enum Result {EMPTY, FULL, PENDING}
}
