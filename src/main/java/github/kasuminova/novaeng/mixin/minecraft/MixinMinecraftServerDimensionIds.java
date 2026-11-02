package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.DimensionTickIds;
import net.minecraft.advancements.FunctionManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.ITickable;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Util;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.chunkio.ChunkIOExecutor;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Hashtable;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.FutureTask;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServerDimensionIds {

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    public Profiler profiler;

    @Shadow
    @Final
    public Queue<FutureTask<?>> futureTaskQueue;
    @Shadow(remap = false)
    public Hashtable<Integer, long[]> worldTickTimes;
    @Shadow
    @Final
    private List<ITickable> tickables;
    @Shadow
    private PlayerList playerList;
    @Shadow
    private int tickCounter;

    @Shadow
    public abstract boolean getAllowNether();

    @Shadow
    public abstract NetworkSystem getNetworkSystem();

    @Shadow
    public abstract FunctionManager getFunctionManager();

    /**
     * @author circulation
     * @reason Iterate primitive dimension IDs while retaining Forge tick hooks, leak-check timing and task order.
     */
    @Overwrite
    public void updateTimeLightAndEntities() {
        this.profiler.startSection("jobs");
        final Queue<FutureTask<?>> lock = this.futureTaskQueue;
        synchronized (lock) {
            while (!this.futureTaskQueue.isEmpty()) {
                Util.runTask(this.futureTaskQueue.poll(), LOGGER);
            }
        }
        this.profiler.endStartSection("levels");
        ChunkIOExecutor.tick();
        final int[] dimensions = DimensionTickIds.get(this.tickCounter % 200 == 0);
        for (final int dimension : dimensions) {
            final long start = System.nanoTime();
            if (dimension == 0 || this.getAllowNether()) {
                final WorldServer world = DimensionManager.getWorld(dimension);
                this.profiler.func_194340_a(() -> world.getWorldInfo().getWorldName());
                if (this.tickCounter % 20 == 0) {
                    this.profiler.startSection("timeSync");
                    this.playerList.sendPacketToAllPlayersInDimension(new SPacketTimeUpdate(world.getTotalWorldTime(),
                        world.getWorldTime(), world.getGameRules().getBoolean("doDaylightCycle")), world.provider.getDimension());
                    this.profiler.endSection();
                }
                this.profiler.startSection("tick");
                FMLCommonHandler.instance().onPreWorldTick(world);
                try {
                    world.tick();
                } catch (final Throwable failure) {
                    final CrashReport report = CrashReport.makeCrashReport(failure, "Exception ticking world");
                    world.addWorldInfoToCrashReport(report);
                    throw new ReportedException(report);
                }
                try {
                    world.updateEntities();
                } catch (final Throwable failure) {
                    final CrashReport report = CrashReport.makeCrashReport(failure, "Exception ticking world entities");
                    world.addWorldInfoToCrashReport(report);
                    throw new ReportedException(report);
                }
                FMLCommonHandler.instance().onPostWorldTick(world);
                this.profiler.endSection();
                this.profiler.startSection("tracker");
                world.getEntityTracker().tick();
                this.profiler.endSection();
                this.profiler.endSection();
            }
            this.worldTickTimes.get(dimension)[this.tickCounter % 100] = System.nanoTime() - start;
        }
        this.profiler.endStartSection("dim_unloading");
        DimensionManager.unloadWorlds(this.worldTickTimes);
        this.profiler.endStartSection("connection");
        this.getNetworkSystem().networkTick();
        this.profiler.endStartSection("players");
        this.playerList.onTick();
        this.profiler.endStartSection("commandFunctions");
        this.getFunctionManager().update();
        this.profiler.endStartSection("tickables");
        for (int i = 0; i < this.tickables.size(); i++) {
            this.tickables.get(i).update();
        }
        this.profiler.endSection();
    }
}
