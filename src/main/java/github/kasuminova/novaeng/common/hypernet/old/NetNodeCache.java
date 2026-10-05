package github.kasuminova.novaeng.common.hypernet.old;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.helper.IMachineController;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.hypernet.old.research.ResearchStation;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import io.netty.util.internal.ThrowableUtil;
import net.minecraft.world.World;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ZenRegister
@ZenClass("novaeng.hypernet.NetNodeCache")
public class NetNodeCache {
    private static final Map<TileMultiblockMachineController, NetNode> CACHED_NODES = new ConcurrentHashMap<>();

    @ZenMethod
    public static DataProcessor getDataProcessor(IMachineController ctrl) {
        return getCache(ctrl.getController(), DataProcessor.class);
    }

    @ZenMethod
    public static Database getDatabase(IMachineController ctrl) {
        return getCache(ctrl.getController(), Database.class);
    }

    @ZenMethod
    public static ResearchStation getResearchStation(IMachineController ctrl) {
        return getCache(ctrl.getController(), ResearchStation.class);
    }

    public static <T extends NetNode> T getCache(TileMultiblockMachineController ctrl, Class<T> type) {
        DynamicMachine machine = ctrl.getFoundMachine();
        if (type == null || machine == null) {
            return null;
        }

        NetNode node = CACHED_NODES.get(ctrl);
        if (type.isInstance(node)) {
            return type.cast(node);
        }

        Class<? extends NetNode> ctrlType = RegistryHyperNet.getNodeType(machine);
        if (ctrlType == null) {
            throw new IllegalStateException(String.format(
                "Invalid NetNode controller type: %s", machine.getRegistryName()));
        }
        if (type != ctrlType) {
            throw new IllegalStateException(String.format(
                "Try to get node type %s, but controller type is %s.", type.getSimpleName(), ctrlType.getSimpleName()));
        }

        synchronized (ctrl) {
            node = CACHED_NODES.get(ctrl);
            if (type.isInstance(node)) {
                return type.cast(node);
            }
            if (node != null) {
                CACHED_NODES.remove(ctrl, node);
            }

            try {
                Constructor<T> constructor = type.getConstructor(TileMultiblockMachineController.class);
                T instance = constructor.newInstance(ctrl);
                instance.readNBT();

                // An invalidated controller is never ticked again, and this map is keyed by
                // identity - the tile does not override equals/hashCode. Caching one would pin
                // it, and through its world field the entire World, for the rest of the session.
                if (!ctrl.isInvalid()) {
                    CACHED_NODES.put(ctrl, instance);
                }
                return instance;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(
                    "Unable to find single parameter constructor in class! Please report this issue to the developers.",
                    e
                );
            } catch (Exception e) {
                NovaEngineeringCore.log.warn(ThrowableUtil.stackTraceToString(e));
                return null;
            }
        }
    }

    public static void removeCache(TileMultiblockMachineController ctrl) {
        CACHED_NODES.remove(ctrl);
    }

    /**
     * Drops every node whose controller belongs to {@code world}.
     *
     * <p>Backstop for {@link #removeCache}: a controller that never reaches
     * {@code invalidate()} - the world was torn down under it, or a mod swapped the
     * tile out without the usual lifecycle - would otherwise keep its world, and
     * every chunk and entity in it, alive for the rest of the session.</p>
     */
    public static void removeWorld(final World world) {
        CACHED_NODES.keySet().removeIf(ctrl -> ctrl.getWorld() == world);
    }

    public static void clearCache() {
        CACHED_NODES.clear();
    }
}
