package github.kasuminova.novaeng.common.util;

import ae2.api.stacks.AEItemKey;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Map;
import net.minecraft.network.PacketBuffer;

public class AEItemStackSet {

    private final Map<Entry, Entry> entries = new Object2ObjectOpenHashMap<>();
    private final List<Entry> entryList = new ObjectArrayList<>();

    public int add(AEItemKey stack) {
        Entry entry = entries.get(new Entry(stack, -1));
        if (entry == null) {
            entry = new Entry(stack, entryList.size());
            entries.put(entry, entry);
            entryList.add(entry);
        }
        return entry.id();
    }

    protected void addInternal(AEItemKey stack) {
        Entry entry = new Entry(stack, entryList.size());
        entryList.add(entry);
        entries.put(entry, entry);
    }

    public AEItemKey get(int id) {
        return entryList.get(id).stack();
    }

    public void writeToBuffer(final ByteBuf buf) {
        buf.writeInt(entryList.size());
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        for (Entry entry : entryList) {
            entry.stack().writeToPacket(packetBuffer);
        }
    }

    public void fromBuffer(final ByteBuf buf) {
        int size = buf.readInt();
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        for (int i = 0; i < size; i++) {
            addInternal(AEItemKey.fromPacket(packetBuffer));
        }
    }

    private record Entry(AEItemKey stack, int id) {

        @Override
        public int hashCode() {
            return stack.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof Entry entry)) {
                return false;
            }
            return stack.equals(entry.stack);
        }

    }

}
