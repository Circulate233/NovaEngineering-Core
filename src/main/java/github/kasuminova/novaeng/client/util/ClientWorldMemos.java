package github.kasuminova.novaeng.client.util;

import github.kasuminova.novaeng.common.util.WeakIdentityMemo;
import net.minecraft.world.World;

/**
 * Scalar render caches must never keep the last disconnected client world alive.
 */
public final class ClientWorldMemos {
    public static final WeakIdentityMemo<World> FOG = new WeakIdentityMemo<>();
    public static final WeakIdentityMemo<World> LIGHTMAP = new WeakIdentityMemo<>();

    private ClientWorldMemos() {
    }

    public static void clearWorld(final World world) {
        FOG.clearIf(world);
        LIGHTMAP.clearIf(world);
    }

    public static void clear() {
        FOG.clear();
        LIGHTMAP.clear();
    }
}
