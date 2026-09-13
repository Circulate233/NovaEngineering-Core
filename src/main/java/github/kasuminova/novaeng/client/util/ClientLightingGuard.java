package github.kasuminova.novaeng.client.util;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import net.minecraft.client.Minecraft;

public final class ClientLightingGuard {

    private static boolean lastEnabled;
    private static boolean renderRefreshPending;
    private static Object lightmapWorld;

    private ClientLightingGuard() {
    }

    public static boolean isActive() {
        return NovaEngCoreConfig.CLIENT.enableFullbright;
    }

    public static boolean consumeRenderRefresh() {
        final boolean enabled = isActive();
        if (enabled != lastEnabled) {
            lastEnabled = enabled;
            renderRefreshPending = true;
        }
        if (renderRefreshPending) {
            renderRefreshPending = false;
            return true;
        }
        return false;
    }

    public static boolean shouldUploadLightmap() {
        final Minecraft minecraft = Minecraft.getMinecraft();
        final Object world = minecraft == null ? null : minecraft.world;
        if (!isActive() || world == null) {
            lightmapWorld = null;
            return true;
        }
        if (world != lightmapWorld) {
            lightmapWorld = world;
            return true;
        }
        return false;
    }
}
