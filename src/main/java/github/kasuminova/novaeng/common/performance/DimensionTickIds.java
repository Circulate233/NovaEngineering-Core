package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.mixin.minecraft.forge.AccessorDimensionManagerWorlds;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLLog;

import java.util.Map;

public final class DimensionTickIds {

    private static final Int2IntOpenHashMap LEAK_COUNTS = new Int2IntOpenHashMap();

    private DimensionTickIds() {
    }

    public static int[] get(final boolean checkLeaks) {
        final Int2ObjectMap<WorldServer> worlds = AccessorDimensionManagerWorlds.nova$loadedWorlds();
        if (checkLeaks) {
            checkLeaks();
        }
        return worlds.keySet().toIntArray();
    }

    public static void checkLeaks() {
        checkLeaks(AccessorDimensionManagerWorlds.nova$loadedWorlds(),
            AccessorDimensionManagerWorlds.nova$weakWorlds(), LEAK_COUNTS);
    }

    static <T extends World> void checkLeaks(final Int2ObjectMap<? extends T> worlds, final Map<T, ?> weakWorlds,
                                             final Int2IntOpenHashMap leakedWorlds) {
        final ObjectArrayList<T> unloaded = new ObjectArrayList<>(weakWorlds.keySet());
        unloaded.removeAll(worlds.values());
        // Forge counts the entire batch before reporting, including identity-hash collisions.
        for (int i = 0; i < unloaded.size(); i++) {
            final int hash = System.identityHashCode(unloaded.get(i));
            if (leakedWorlds.addTo(hash, 1) == Integer.MAX_VALUE) {
                leakedWorlds.put(hash, Integer.MAX_VALUE);
                throw new IllegalArgumentException("too many occurrences: 2147483648");
            }
        }
        for (int i = 0; i < unloaded.size(); i++) {
            final T world = unloaded.get(i);
            final int count = leakedWorlds.get(System.identityHashCode(world));
            if (count % 5 == 0) {
                if (count == 5) {
                    FMLLog.log.debug("The world {} ({}) may have leaked: first encounter (5 occurrences).\n",
                        Integer.toHexString(System.identityHashCode(world)), world.getWorldInfo().getWorldName());
                } else {
                    FMLLog.log.debug("The world {} ({}) may have leaked: seen {} times.\n",
                        Integer.toHexString(System.identityHashCode(world)), world.getWorldInfo().getWorldName(), count);
                }
            }
        }
    }

}
