package github.kasuminova.novaeng.common.handler;

import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftbutilities.FTBUtilities;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

public class ForceChunkHandler {

    public static final ForceChunkHandler INSTANCE = new ForceChunkHandler();
    Random random = new Random();
    final int randomX = random.nextInt(100000) + 150000;
    final int randomY = random.nextInt(100000) + 150000;
    int time = 0;

    private void request(MinecraftServer server) {
        for (DimensionType i : DimensionManager.getRegisteredDimensions().keySet()) {
            int id = i.getId();
            if (id != 2) {
                if (DimensionManager.isDimensionRegistered(id)) {
                    WorldServer worldServer = server.getWorld(id);
                    if (worldServer != null) {
                        ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(FTBUtilities.INST, worldServer, ForgeChunkManager.Type.NORMAL);
                        if (ticket != null) {
                            worldServer.addScheduledTask(() -> {
                                if (worldServer != null && ticket != null) {
                                    ForgeChunkManager.forceChunk(ticket, new ChunkPos(randomX, randomY));
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (time % 50 == 0) {
                request(Universe.get().server);
            }
            ++time;
        }
    }

}
