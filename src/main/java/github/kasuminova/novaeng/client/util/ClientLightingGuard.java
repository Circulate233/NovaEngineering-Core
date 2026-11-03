package github.kasuminova.novaeng.client.util;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.common.util.MixinDecisions;
import net.coderbot.iris.Iris;
import net.coderbot.iris.apiimpl.IrisApiV0Impl;
import net.coderbot.iris.config.IrisConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

public final class ClientLightingGuard {

    private static boolean lastEnabled;
    private static boolean renderRefreshPending;

    private ClientLightingGuard() {
    }

    public static boolean isActive() {
        if (!NovaEngCoreConfig.CLIENT.enableFullbright) {
            return false;
        }
        return !MixinDecisions.actiniumLoaded || isIrisShadersEnabled();
    }

    @Optional.Method(modid = "actinium")
    public static boolean isIrisEnabled() {
        return Iris.enabled;
    }

    @Optional.Method(modid = "actinium")
    public static boolean isIrisShadersEnabled() {
        if (!isIrisEnabled()) {
            return true;
        }
        if (!Iris.isInitialized()) {
            return false;
        }
        final IrisConfig config = Iris.getIrisConfig();
        return config != null && !config.areShadersEnabled() && !IrisApiV0Impl.INSTANCE.isShaderPackInUse();
    }

    public static boolean shouldProcessLighting(final World world) {
        return !world.isRemote || (MixinDecisions.actiniumLoaded && isIrisEnabled()) || !isActive();
    }

    public static boolean consumeRenderRefresh() {
        final boolean enabled = isActive();
        if (enabled != lastEnabled) {
            lastEnabled = enabled;
            ClientWorldMemos.LIGHTMAP.clear();
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
        final World world = minecraft == null ? null : minecraft.world;
        if (!isActive() || world == null) {
            ClientWorldMemos.LIGHTMAP.clear();
            return true;
        }
        return ClientWorldMemos.LIGHTMAP.select(world);
    }
}
