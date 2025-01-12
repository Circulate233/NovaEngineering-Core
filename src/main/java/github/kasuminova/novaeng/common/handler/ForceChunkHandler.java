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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class ForceChunkHandler {

    public static final ForceChunkHandler INSTANCE = new ForceChunkHandler();
    Random random = new Random();
    final int randomX = random.nextInt(100000) + 100000;
    final int randomY = random.nextInt(100000) + 100000;

    private void requestTicket(MinecraftServer server) {
        List<Integer> ids = Arrays.asList(-1,0,1);
        for (DimensionType i : DimensionManager.getRegisteredDimensions().keySet()) {
            if (i.getId() != 2) {
                WorldServer worldServer = server.getWorld(i.getId());
                if (worldServer.getTotalWorldTime() % 40 == 0) {
                    ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(FTBUtilities.INST, worldServer, ForgeChunkManager.Type.NORMAL);
                    Objects.requireNonNull(ticket);
                    ForgeChunkManager.forceChunk(ticket, new ChunkPos(randomX, randomY));
                }
            }

        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = Universe.get().server;
        requestTicket(server);
    }

}
