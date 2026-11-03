package github.kasuminova.novaeng.mixin.endercore;

import com.enderio.core.common.util.WorldCache;
import com.google.common.collect.BiMap;

import github.kasuminova.novaeng.common.util.collection.Object2IntBiMap;
import github.kasuminova.novaeng.common.util.collection.Object2IntHashBiMap;
import github.kasuminova.novaeng.common.util.collection.PrimitiveBiMaps;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.Set;

@Mixin(value = WorldCache.class, remap = false)
public abstract class MixinWorldCachePrimitiveBiMap {
    @Shadow
    protected BiMap<String, Integer> nameToID;
    @Shadow
    protected BitSet usedIDs;
    @Shadow
    protected Set<Integer> blockedIDs;
    @Shadow
    protected BiMap<Object, String> objToName;

    @Redirect(
        method = "<init>(Ljava/lang/String;Z)V",
        at =
        @At(
            value = "FIELD",
            target =
                "Lcom/enderio/core/common/util/WorldCache;nameToID:Lcom/google/common/collect/BiMap;",
            opcode = Opcodes.PUTFIELD),
        require = 1)
    private void nova$primitiveTable(
        final WorldCache<?> owner, final BiMap<String, Integer> original) {
        final Object2IntHashBiMap<String> replacement = new Object2IntHashBiMap<>(16);
        replacement.putAll(original);
        nameToID = replacement;
    }

    @Shadow
    public abstract String getName(Object object);

    @Shadow
    public abstract Object getObject(String name);

    /**
     * @author NovaEngineering
     * @reason Store world-cache IDs through the primitive table.
     */
    @Overwrite
    protected void setID(final String name, final int id) {
        PrimitiveBiMaps.putObjectInt(nameToID, name, id);
        usedIDs.set(id, true);
    }

    /**
     * @author NovaEngineering
     * @reason Resolve the numeric cache ID directly.
     */
    @Overwrite
    public int getID(final Object object) {
        return PrimitiveBiMaps.getIntValue(nameToID, getName(object));
    }

    /**
     * @author NovaEngineering
     * @reason Resolve cached objects without boxing their ID.
     */
    @Overwrite
    public Object getObject(final int id) {
        return getObject((String) PrimitiveBiMaps.getIntKey(nameToID.inverse(), id));
    }

    @Shadow
    protected abstract void generateIDs();

    @Shadow
    protected abstract void setID(String name);

    @Redirect(
        method = "<init>(Ljava/lang/String;Z)V",
        at =
        @At(
            value = "FIELD",
            target =
                "Lcom/enderio/core/common/util/WorldCache;blockedIDs:Ljava/util/Set;",
            opcode = Opcodes.PUTFIELD),
        require = 1)
    private void nova$primitiveBlockedIds(WorldCache<?> owner, Set<Integer> original) {
        blockedIDs = new IntOpenHashSet();
    }

    /**
     * @author NovaEngineering
     * @reason Load persisted IDs without boxing integer keys or values.
     */
    @Overwrite
    protected void loadData(File file) throws IOException {
        if (!file.createNewFile()) {
            NBTTagCompound tag = null;
            try {
                tag = CompressedStreamTools.read(file);
            } catch (Exception e) {
                this.generateIDs();
                return;
            }
            if (tag != null && tag.hasKey("ItemData")) {
                NBTTagList list = tag.getTagList("ItemData", 10);
                for (int i = 0; i < list.tagCount(); ++i) {
                    NBTTagCompound dataTag = list.getCompoundTagAt(i);
                    int id = dataTag.getInteger("V");
                    String name = dataTag.getString("K");
                    PrimitiveBiMaps.putObjectInt(nameToID, name, id);
                    if (!this.objToName.containsValue(name)) continue;
                    this.usedIDs.set(id, true);
                }
                for (int id : tag.getIntArray("BlockedItemIds")) {
                    ((IntSet) blockedIDs).add(id);
                }
                this.blockOldIDs();
                this.mergeNewIDs();
                return;
            }
        }
        this.generateIDs();
    }

    /**
     * @author NovaEngineering
     * @reason Save numeric IDs through native entries and primitive arrays.
     */
    @Overwrite
    protected void saveData(final File file) throws IOException {
        final NBTTagCompound data = new NBTTagCompound();
        final NBTTagList dataList = new NBTTagList();
        for (Object2IntMap.Entry<String> entry :
            ((Object2IntBiMap<String>) nameToID).object2IntEntrySet()) {
            if (entry.getKey() == null) continue;
            final NBTTagCompound tag = new NBTTagCompound();
            tag.setString("K", entry.getKey());
            tag.setInteger("V", entry.getIntValue());
            dataList.appendTag(tag);
        }
        data.setTag("ItemData", dataList);
        data.setIntArray("BlockedItemIds", ((IntSet) blockedIDs).toIntArray());
        CompressedStreamTools.safeWrite(data, file);
    }

    /**
     * @author NovaEngineering
     * @reason Block and remove retired IDs without boxed entry values.
     */
    @Overwrite
    protected void blockOldIDs() {
        final var entries = ((Object2IntBiMap<String>) nameToID).object2IntEntrySet();
        final IntSet blocked = (IntSet) blockedIDs;
        for (Object2IntMap.Entry<String> entry : entries) {
            if (!objToName.containsValue(entry.getKey())) blocked.add(entry.getIntValue());
        }
        final var iterator = entries.iterator();
        while (iterator.hasNext()) {
            if (blocked.contains(iterator.next().getIntValue())) iterator.remove();
        }
    }

    /**
     * @author NovaEngineering
     * @reason Check existing names without retrieving an unused boxed ID.
     */
    @Overwrite
    protected void mergeNewIDs() {
        for (String name : objToName.values()) {
            if (nameToID.containsKey(name)) setID(name);
        }
    }

    /**
     * @author NovaEngineering
     * @reason Probe occupied IDs with primitive set and inverse-map operations.
     */
    @Overwrite
    protected int getNextAvailableID(final String name) {
        final Object2IntBiMap<String> ids = (Object2IntBiMap<String>) nameToID;
        if (ids.containsKey(name)) return ids.getIntRequired(name);
        int next = usedIDs.nextClearBit(0);
        while (((IntSet) blockedIDs).contains(next) || ids.containsValue(next)) {
            next++;
        }
        return next;
    }
}
