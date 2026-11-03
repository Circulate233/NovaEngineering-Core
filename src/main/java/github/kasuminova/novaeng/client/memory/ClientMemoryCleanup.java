package github.kasuminova.novaeng.client.memory;

import github.kasuminova.novaeng.client.render.LitItemQuadScratch;
import github.kasuminova.novaeng.client.render.TileEntityRenderCulling;
import github.kasuminova.novaeng.common.util.MixinDecisions;
import github.kasuminova.novaeng.mixin.neofontrender.AccessorSignOcclusionCuller;
import hellfirepvp.modularmachinery.common.base.Mods;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import neofontrender.client.render.sign.SignOcclusionCuller;
import net.minecraft.world.World;

/**
 * World-owned client references are released only from the Minecraft thread.
 */
public final class ClientMemoryCleanup {
    private static final ObjectArrayFIFOQueue<World> UNLOADED = new ObjectArrayFIFOQueue<>();
    private static boolean quadScratchInstalled;

    private ClientMemoryCleanup() {
    }

    public static void clearWorld(final World world) {
        TileEntityRenderCulling.clearWorld(world);
        if (MixinDecisions.neoFontRenderLoaded && AccessorSignOcclusionCuller.nova$currentWorld() == world) {
            SignOcclusionCuller.beginFrame(null);
        }
        UNLOADED.enqueue(world);
    }

    public static void tick() {
        if (!quadScratchInstalled) {
            if (MixinDecisions.actiniumLoaded) LitItemQuadScratch.install();
            quadScratchInstalled = true;
        }
        final boolean mmceModels = MixinDecisions.mmceLoaded && Mods.GECKOLIB.isPresent();
        final boolean changed = !UNLOADED.isEmpty();
        while (!UNLOADED.isEmpty()) {
            final World world = UNLOADED.dequeue();
            if (mmceModels) MmceRenderMemory.clearWorld(world);
        }
        if (mmceModels) MmceRenderMemory.tick();
        if (changed && MixinDecisions.neoFontRenderLoaded) DynamicFontCaches.clearDynamicCaches();
    }
}
