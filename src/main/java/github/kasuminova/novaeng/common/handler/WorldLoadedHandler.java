package github.kasuminova.novaeng.common.handler;

import crafttweaker.annotations.ZenRegister;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.Map;
import java.util.Random;

import static github.kasuminova.novaeng.NovaEngCoreConfig.SERVER;

@ZenRegister
@ZenClass("novaeng.WorldLoadedHandler")
public class WorldLoadedHandler {

    public static final WorldLoadedHandler INSTANCE = new WorldLoadedHandler();
    public static final Map<Integer, ForgeChunkManager.Ticket> map = new Object2ObjectOpenHashMap<>();
    public static final IntSet REGISTERED_DIMENSIONS = new IntLinkedOpenHashSet();
    public static final IntSet ERRORWROLD = new IntLinkedOpenHashSet();
    public static boolean init = true;
    static Random random = new Random();
    static final int randomX = random.nextInt(100000) + 150000;
    static final int randomY = random.nextInt(100000) + 150000;
    public static final ChunkPos chunk = new ChunkPos(randomX, randomY);
    int time = 0;

    public static void loadWorld(int... id) {
        for (int i : id) {
            loadWorld(i);
        }
    }

    @ZenMethod
    public static void loadWorld(int id) {
        if (ERRORWROLD.contains(id)) return;
        REGISTERED_DIMENSIONS.add(id);
        if (DimensionManager.isDimensionRegistered(id)) {
            WorldServer worldServer = DimensionManager.getWorld(id, true);
            if (worldServer == null) {
                DimensionManager.initDimension(id);
            }
            if (worldServer != null) {
                DimensionManager.keepDimensionLoaded(id, true);
            } else {
                ERRORWROLD.add(id);
            }
        }
    }

    private void request() {
        if (init) {
            loadWorld(0, 1, -1);
            init = false;
        } else {
            REGISTERED_DIMENSIONS.forEach(WorldLoadedHandler::loadWorld);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        switch (event.phase) {
            case START -> {
                if (SERVER.forceChunkHandler) {
                    if (time % 100 == 0) {
                        request();
                    }
                    ++time;
                }
            }
        }
    }
}
