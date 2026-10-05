package github.kasuminova.novaeng.common.network;

import github.kasuminova.mmce.common.util.concurrent.Action;
import github.kasuminova.mmce.common.util.concurrent.ActionExecutor;
import github.kasuminova.novaeng.common.mod.Mods;
import hellfirepvp.modularmachinery.ModularMachinery;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import mekanism.common.Mekanism;
import net.minecraftforge.fml.common.Optional;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class ParallelNetworkManager {

    private final Map<Object, Queue<ActionExecutor>> groupQueues = new ConcurrentHashMap<>();
    private final ReferenceSet<Object> blacklistChannels = new ReferenceOpenHashSet<>();

    public void init() {
        if (Mods.MEKCEU.loaded()) {
            initializeMekanismCEuBlacklist();
        }
    }

    /**
     * MekCEu is async, so we dont need proxy that.
     */
    @Optional.Method(modid = "mekanism")
    private void initializeMekanismCEuBlacklist() {
        addBlacklistChannel(Mekanism.packetHandler.netHandler);
    }

    public void offerAction(final Object group, final Action action) {
        offerAction(group, action, 0);
    }

    public void offerAction(final Object group, final Action action, final int priority) {
        Queue<ActionExecutor> queue = groupQueues.computeIfAbsent(group, k -> new PriorityQueue<>());
        synchronized (queue) {
            queue.offer(new ActionExecutor(action, priority));
        }
    }

    public void execute() {
        ModularMachinery.EXECUTE_MANAGER.addTask(() -> {
            for (final Queue<ActionExecutor> queue : groupQueues.values()) {
                synchronized (queue) {
                    ActionExecutor action;
                    while ((action = queue.poll()) != null) {
                        action.run();
                    }
                }
            }
        });
    }

    public boolean isBlacklistChannel(final Object channel) {
        return blacklistChannels.contains(channel);
    }

    public void addBlacklistChannel(final Object channel) {
        blacklistChannels.add(channel);
    }

}
