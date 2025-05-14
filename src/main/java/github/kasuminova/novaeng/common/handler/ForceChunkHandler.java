package github.kasuminova.novaeng.common.handler;

import com.feed_the_beast.ftblib.lib.data.Universe;
import mekanism.common.Mekanism;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

import static github.kasuminova.novaeng.common.config.NovaEngCoreConfig.SERVER;

public class ForceChunkHandler {

    public static final ForceChunkHandler INSTANCE = new ForceChunkHandler();
    static Random random = new Random();
    static final int randomX = random.nextInt(100000) + 150000;
    static final int randomY = random.nextInt(100000) + 150000;
    public static final ChunkPos chunk = new ChunkPos(randomX, randomY);
    int time = 0;
    public static final Map<Integer,ForgeChunkManager.Ticket> map = new HashMap<>();
    private static final Set<DimensionType> REGISTERED_DIMENSIONS = new HashSet<>();

    private void request(MinecraftServer server) {
        if (REGISTERED_DIMENSIONS.isEmpty()){
            REGISTERED_DIMENSIONS.addAll(DimensionManager.getRegisteredDimensions().keySet());
        }
        for (DimensionType i : REGISTERED_DIMENSIONS) {
            int id = i.getId();
            if (id != 2) {
                if (DimensionManager.isDimensionRegistered(id)) {
                    WorldServer worldServer = server.getWorld(id);
                    if (ForgeChunkManager.getPersistentChunksFor(worldServer).containsKey(chunk))continue;
                    if (map.get(id) == null){
                        ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(Mekanism.instance, worldServer, ForgeChunkManager.Type.NORMAL);
                        if (ticket != null) {
                            ticket.setChunkListDepth(1);
                            map.put(id, ticket);
                        }
                    }
                    if (map.get(id) != null) {
                        worldServer.addScheduledTask(() -> ForgeChunkManager.forceChunk(map.get(id), chunk));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (!SERVER.ForceChunkHandler)return;
        if (event.phase == TickEvent.Phase.START) {
            if (time % 100 == 0) {
                request(Universe.get().server);
            }
            ++time;
        }
    }

}
