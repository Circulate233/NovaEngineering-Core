package github.kasuminova.novaeng.client.render.construction;

import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.client.render.construction.ConstructionAnimation.SectionMesh;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.End;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.Geometry;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.Hud;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.Visibility;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import static github.kasuminova.novaeng.client.render.construction.ConstructionAnimation.MIN_OPACITY;

@SideOnly(Side.CLIENT)
public final class ClientConstructionEffects {
    public static final ClientConstructionEffects INSTANCE = new ClientConstructionEffects();
    private static final long FRAME_BUILD_NANOS = 1_500_000L;
    private static final boolean ACTINIUM = Loader.isModLoaded("actinium");

    private final Long2ObjectOpenHashMap<ConstructionAnimation> tasks = new Long2ObjectOpenHashMap<>(4);
    private final ObjectArrayList<SectionMesh> visible = new ObjectArrayList<>();
    private ConstructionMeshes meshes;
    private ConstructionBatches batches;
    private ConstructionGlState state;
    private WorldClient world;
    private EntityPlayerSP player;
    private long hudTask;
    private long tick;
    private int frame;
    private boolean vbo;

    private ClientConstructionEffects() {
    }

    public static void accept(final ConstructionEffectPayload payload) {
        final var mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null || mc.world.provider.getDimension() != payload.dimension()) return;
        INSTANCE.receive(mc, payload);
    }

    private static double interpolate(final double factor, final double min, final double max) {
        return min + (max - min) * factor;
    }

    private void receive(final Minecraft mc, final ConstructionEffectPayload payload) {
        useWorld(mc.world, mc.player);
        try {
            switch (payload) {
                case Geometry geometry -> {
                    var task = tasks.get(geometry.taskId());
                    final var layout = geometry.layout();
                    if (layout != null) {
                        if (task == null) {
                            task = new ConstructionAnimation(layout.dismantle(), layout.startTick(), layout.wireTicks(), layout.chunks(), layout.area());
                            tasks.put(geometry.taskId(), task);
                        }
                        task.hidden = false;
                    } else if (task == null) return;
                    task.geometry(geometry, tick);
                }
                case Visibility visibility -> {
                    final var task = tasks.get(visibility.taskId());
                    if (task != null) task.hidden = !visibility.visible();
                }
                case Hud hud -> {
                    var task = tasks.get(hud.taskId());
                    if (task == null) {
                        final var layout = hud.layout();
                        if (layout == null) return;
                        task = new ConstructionAnimation(layout.dismantle(), layout.startTick(), layout.wireTicks(), layout.chunks(), layout.area());
                        tasks.put(hud.taskId(), task);
                    }
                    task.owner = true;
                    final var status = hud.status();
                    task.metrics(status.missing(), status.errors(), status.unloaded());
                    hudTask = Math.max(hudTask, hud.taskId());
                }
                case End end -> {
                    final var task = tasks.get(end.taskId());
                    if (task == null) return;
                    if (end.cancelled()) drop(end.taskId());
                    else {
                        task.complete = end.successful();
                        task.hudText = null;
                        task.finish(end.serverTick(), tick);
                    }
                }
            }
        } catch (RuntimeException failure) {
            drop(payload.taskId());
            NovaEngineeringCore.log.error("Could not install construction effect for task {}", payload.taskId(), failure);
        }
    }

    private void drop(final long id) {
        final var task = tasks.remove(id);
        if (task != null) dispose(task);
        if (hudTask == id) hudTask = 0;
        if (tasks.isEmpty()) clearScratch();
    }

    private void dispose(final ConstructionAnimation task) {
        if (meshes != null) meshes.cancel(task);
        task.dispose();
    }

    private void useWorld(final WorldClient nextWorld, final EntityPlayerSP nextPlayer) {
        if (world == nextWorld && player == nextPlayer) return;
        clear();
        world = nextWorld;
        player = nextPlayer;
    }

    private void clear() {
        for (final var task : tasks.values()) task.dispose();
        tasks.clear();
        clearScratch();
        world = null;
        player = null;
        hudTask = 0;
        tick = 0;
        frame = 0;
    }

    private void clearScratch() {
        if (meshes != null) meshes.close();
        if (batches != null) batches.close();
        if (state != null) state.close();
        meshes = null;
        batches = null;
        state = null;
        visible.clear();
    }

    @SubscribeEvent
    public void worldUnload(final WorldEvent.Unload event) {
        if (!(event.getWorld() instanceof WorldClient unloaded)) return;
        final var mc = Minecraft.getMinecraft();
        if (mc.isCallingFromMinecraftThread()) {
            if (world == unloaded) clear();
        } else {
            mc.addScheduledTask(() -> {
                if (world == unloaded) clear();
            });
        }
    }

    @SubscribeEvent
    public void chunkUnload(final ChunkEvent.Unload event) {
        final var chunk = event.getChunk();
        if (!(chunk.getWorld() instanceof WorldClient unloaded)) return;
        final int x = chunk.x;
        final int z = chunk.z;
        final var mc = Minecraft.getMinecraft();
        if (mc.isCallingFromMinecraftThread()) {
            if (world == unloaded) forgetChunk(x, z);
        } else {
            mc.addScheduledTask(() -> {
                if (world == unloaded) forgetChunk(x, z);
            });
        }
    }

    private void forgetChunk(final int x, final int z) {
        for (final var task : tasks.values()) task.forgetChunk(x, z);
    }

    @SubscribeEvent
    public void tick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        final var mc = Minecraft.getMinecraft();
        useWorld(mc.world, mc.player);
        if (world == null) return;
        if (!mc.isGamePaused()) tick++;
        if (tasks.isEmpty()) return;
        final var iterator = tasks.long2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            final var entry = iterator.next();
            final var task = entry.getValue();
            if (task.expired(tick)) {
                if (hudTask == entry.getLongKey()) hudTask = 0;
                dispose(task);
                iterator.remove();
            } else {
                task.releaseExpiredMeshes(tick);
            }
        }
        if (tasks.isEmpty()) clearScratch();
    }

    @SubscribeEvent
    public void hud(final RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        final var mc = Minecraft.getMinecraft();
        final var viewer = mc.player;
        if (mc.currentScreen != null || mc.world != world || viewer == null || viewer != player) return;
        final var task = tasks.get(hudTask);
        final float partial = event.getPartialTicks();
        if (task == null || !task.area.withinRenderDistance(
            interpolate(partial, viewer.lastTickPosX, viewer.posX), interpolate(partial, viewer.lastTickPosY, viewer.posY),
            interpolate(partial, viewer.lastTickPosZ, viewer.posZ))) return;
        final boolean success = task.complete && task.missing == 0 && task.errors == 0 && task.unloaded == 0;
        if (task.hudText == null) {
            final String key = "message.assembly.hud." + (task.dismantle ? "dismantle" : "build");
            task.hudText = success ? I18n.format(key + "_complete") : I18n.format(key, task.missing, task.errors, task.unloaded);
        }
        final int color = success ? 0x65ef85 : task.errors != 0 || task.unloaded != 0 ? 0xf0ae67 : 0xf4f8ff;
        final int width = event.getResolution().getScaledWidth();
        final int height = event.getResolution().getScaledHeight();
        mc.fontRenderer.drawStringWithShadow(task.hudText, (width - mc.fontRenderer.getStringWidth(task.hudText)) * 0.5F, height - 53, color);
    }

    @SubscribeEvent
    public void render(final RenderWorldLastEvent event) {
        if (tasks.isEmpty()) return;
        final var mc = Minecraft.getMinecraft();
        final var camera = mc.getRenderViewEntity();
        if (mc.world != world || camera == null || mc.player == null) return;
        final Profiler profiler = mc.profiler;
        profiler.startSection("novaConstruction");
        try {
            renderScene(camera, mc.player, event.getPartialTicks());
        } finally {
            if (batches != null) batches.clear();
            visible.clear();
            profiler.endSection();
        }
    }

    private void renderScene(final Entity camera, final EntityPlayerSP viewer, final float partial) {
        frame++;
        final double x = interpolate(partial, camera.lastTickPosX, camera.posX);
        final double y = interpolate(partial, camera.lastTickPosY, camera.posY);
        final double z = interpolate(partial, camera.lastTickPosZ, camera.posZ);
        final double viewerX = viewer == camera ? x : interpolate(partial, viewer.lastTickPosX, viewer.posX);
        final double viewerY = viewer == camera ? y : interpolate(partial, viewer.lastTickPosY, viewer.posY);
        final double viewerZ = viewer == camera ? z : interpolate(partial, viewer.lastTickPosZ, viewer.posZ);
        final double now = tick + (double) partial;
        ClippingHelper frustum = null;
        for (final var task : tasks.values()) {
            if (task.hidden || task.sections.isEmpty() || !task.area.withinRenderDistance(viewerX, viewerY, viewerZ))
                continue;
            final float wire = (float) task.wireOpacity(now);
            final boolean prepareWire = task.prepareOutline(now);
            final boolean prepareShell = task.prepareShell(now);
            task.sampleOpacity(now);
            for (final var section : task.sections.values()) {
                section.prepareWire = prepareWire && section.wire.version != section.version;
                boolean needed = wire > MIN_OPACITY || section.prepareWire;
                for (final var part : section.parts) {
                    final float opacity = (float) task.opacities[part.region];
                    part.opacity = opacity;
                    needed |= opacity > MIN_OPACITY || prepareShell && part.solid() && part.version != section.version;
                }
                if (!needed) continue;
                if (frustum == null) frustum = ClippingHelperImpl.getInstance();
                if (!section.inFrustum(frustum, x, y, z)) continue;
                section.seenFrame = frame;
                section.prepareShell = prepareShell;
                section.wire.opacity = wire;
                visible.add(section);
            }
        }
        if (visible.isEmpty()) {
            if (meshes != null) meshes.cancel();
            return;
        }
        // Actinium otherwise uploads the client array again on every draw in its core profile.
        final boolean useVbo = OpenGlHelper.useVbo() || ACTINIUM && OpenGlHelper.vboSupported;
        if (meshes == null) {
            meshes = new ConstructionMeshes();
            batches = new ConstructionBatches();
            state = new ConstructionGlState(ACTINIUM);
        } else if (vbo != useVbo) {
            meshes.cancel();
            for (final var task : tasks.values()) task.invalidateMeshes();
        }
        vbo = useVbo;
        meshes.beginFrame();
        state.captureBuffer();
        try {
            upload(System.nanoTime() + FRAME_BUILD_NANOS);
            batches.collect(visible);
            if (batches.empty()) return;
            state.capture();
            try {
                state.prepare();
                GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
                try {
                    state.prepareArrays();
                    batches.draw(x, y, z, state);
                } finally {
                    GL11.glPopClientAttrib();
                }
            } finally {
                state.restore();
            }
        } finally {
            state.restoreBuffer();
        }
    }

    private void upload(final long deadline) {
        boolean allowPrefetch = true;
        if (meshes.prefetching()) {
            active:
            for (int i = 0; i < visible.size(); i++) {
                final var section = visible.get(i);
                if (section.wire.opacity > MIN_OPACITY && section.wire.version != section.version) {
                    allowPrefetch = false;
                    break;
                }
                for (final var part : section.parts) {
                    if (part.opacity > MIN_OPACITY && part.version != section.version) {
                        allowPrefetch = false;
                        break active;
                    }
                }
            }
        }
        if (!meshes.resume(frame, deadline, vbo, allowPrefetch)) return;
        for (int i = 0; i < visible.size(); i++) {
            final var section = visible.get(i);
            if (section.wire.opacity > MIN_OPACITY && !meshes.build(section, section.wire, deadline, vbo)) return;
            for (final var part : section.parts) {
                if (part.opacity > MIN_OPACITY && !meshes.build(section, part, deadline, vbo)) return;
            }
        }
        for (int i = 0; i < visible.size(); i++) {
            final var section = visible.get(i);
            if (section.prepareWire && !meshes.build(section, section.wire, deadline, vbo)) return;
            if (!section.prepareShell) continue;
            for (final var part : section.parts) {
                if (part.solid() && !meshes.build(section, part, deadline, vbo)) return;
            }
        }
    }
}
