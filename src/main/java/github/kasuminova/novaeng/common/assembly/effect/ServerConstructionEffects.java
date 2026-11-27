package github.kasuminova.novaeng.common.assembly.effect;

import com.sumirelabs.pulsar.world.PulsarWorld;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.End;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.Geometry;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.Hud;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.Layout;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.RegionTime;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.Section;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.Status;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.Visibility;
import github.kasuminova.novaeng.common.network.PktConstructionEffect;
import github.kasuminova.novaeng.novaeng_core.Tags;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.shorts.Short2ByteOpenHashMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ServerConstructionEffects {
    private static final BitSet NO_CELLS = new BitSet();
    private static final long SYNC_NANOS = 1_000_000L;
    private static final int BYTES_PER_TICK = 64 * 1024;
    private static final int PACKETS_PER_TICK = 128;
    private static final int MAX_LIGHT_WAIT_TICKS = 40;
    private static @Nullable Session current;

    private ServerConstructionEffects() {
    }

    public static Effect create(EntityPlayerMP owner, BlockPos anchor) {
        var world = owner.getServerWorld();
        var session = session(owner.server);
        var index = session.worlds.computeIfAbsent(world, _ -> new WorldIndex(world));
        var effect = new Effect(session, index, owner);
        effect.includeInArea(anchor);
        return effect;
    }

    /**
     * Publish after the previous vanilla player chunk update, before starting new block changes.
     */
    public static void tick(MinecraftServer server) {
        var active = current;
        if (active == null || active.server != server) return;
        active.tick();
    }

    public static void stop() {
        current = null;
    }

    private static Session session(MinecraftServer server) {
        if (!server.isCallingFromMinecraftThread())
            throw new IllegalStateException("Construction effects require the server thread");
        var active = current;
        if (active == null) current = active = new Session(server);
        if (active.server != server) throw new IllegalStateException("Another server owns the construction effects");
        return active;
    }

    @SubscribeEvent
    public static void watch(ChunkWatchEvent.Watch event) {
        watch(event, true);
    }

    @SubscribeEvent
    public static void unwatch(ChunkWatchEvent.UnWatch event) {
        watch(event, false);
    }

    private static void watch(ChunkWatchEvent event, boolean watching) {
        var active = current;
        if (active == null) return;
        var chunk = event.getChunkInstance();
        var world = chunk != null ? chunk.getWorld() : event.getPlayer().world;
        var index = active.worlds.get(world);
        if (index == null) return;
        var pos = event.getChunk();
        var sectors = index.chunks.get(ChunkPos.asLong(pos.x, pos.z));
        if (sectors == null) return;
        var player = event.getPlayer();
        for (var sector : sectors) {
            if (watching) sector.watch(player);
            else sector.unwatch(player);
        }
    }

    @SubscribeEvent
    public static void worldUnloaded(WorldEvent.Unload event) {
        var active = current;
        if (active != null && event.getWorld() instanceof WorldServer world) active.worlds.remove(world);
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        var active = current;
        if (active == null) return;
        var iterator = active.audiences.iterator();
        while (iterator.hasNext()) {
            var recipient = iterator.next();
            if (recipient.player != event.player || recipient.effect.dimension != event.fromDim) continue;
            iterator.remove();
            recipient.detach();
        }
    }

    private enum WorkResult {DONE, CONTINUE, WAIT}

    @FunctionalInterface
    private interface Work {
        WorkResult advance();
    }

    @FunctionalInterface
    private interface ChunkLightReadiness {
        boolean canSend(long chunk);
    }

    private static final class Session {
        final MinecraftServer server;
        final Reference2ObjectOpenHashMap<WorldServer, WorldIndex> worlds = new Reference2ObjectOpenHashMap<>();
        final ReferenceOpenHashSet<Audience> audiences = new ReferenceOpenHashSet<>();
        final ObjectArrayFIFOQueue<Work> work = new ObjectArrayFIFOQueue<>();
        long tick;
        long nextId;
        int remainingBytes;
        int remainingPackets;

        Session(MinecraftServer server) {
            this.server = server;
        }

        void tick() {
            if (audiences.isEmpty() && work.isEmpty()) return;
            long started = System.nanoTime();
            tick = Integer.toUnsignedLong(server.getTickCounter());
            remainingBytes = BYTES_PER_TICK;
            remainingPackets = PACKETS_PER_TICK;
            var viewers = audiences.iterator();
            while (viewers.hasNext()) {
                var recipient = viewers.next();
                var effect = recipient.effect;
                if (!effect.live() || !effect.connected(recipient.player, recipient.connection)) {
                    viewers.remove();
                    recipient.detach();
                } else if (!effect.retiring && !effect.cancelled() && !recipient.viewers.isEmpty())
                    recipient.updateDistance();
            }
            int waiting = 0;
            while (!work.isEmpty() && waiting < work.size() && System.nanoTime() - started < SYNC_NANOS) {
                var next = work.dequeue();
                switch (next.advance()) {
                    case DONE -> waiting = 0;
                    case CONTINUE -> {
                        work.enqueue(next);
                        waiting = 0;
                    }
                    case WAIT -> {
                        work.enqueue(next);
                        waiting++;
                    }
                }
            }
        }

        boolean send(EntityPlayerMP player, ConstructionEffectPayload payload) {
            return send(player, payload, null);
        }

        boolean send(EntityPlayerMP player, ConstructionEffectPayload payload, @Nullable End after) {
            int bytes = ConstructionEffectPayload.bytes(payload) + 1;
            int packets = 1;
            if (after != null) {
                bytes += ConstructionEffectPayload.bytes(after) + 1;
                packets++;
            }
            if (packets > remainingPackets || bytes > remainingBytes) return false;
            remainingPackets -= packets;
            remainingBytes -= bytes;
            PktConstructionEffect.send(player, payload);
            if (after != null) PktConstructionEffect.send(player, after);
            return true;
        }
    }

    private record WorldIndex(WorldServer world, Long2ObjectOpenHashMap<ReferenceOpenHashSet<Sector>> chunks,
                              @Nullable ChunkLightReadiness lighting) {
        WorldIndex(WorldServer world) {
            this(world, new Long2ObjectOpenHashMap<>(), Loader.isModLoaded("pulsar") ? PulsarLighting.forWorld(world) : null);
        }
    }

    private static final class PulsarLighting {
        static @Nullable ChunkLightReadiness forWorld(WorldServer world) {
            var manager = ((PulsarWorld) world).pulsar$getLightManager();
            if (manager == null) return null;
            return chunk -> {
                int x = (int) chunk;
                int z = (int) (chunk >>> 32);
                return manager.canSendUpdatedChunkLight(x, z) || manager.getLoadedChunk(x, z) == null;
            };
        }
    }

    private record SectorKey(Section section, int quadrant) {
        static SectorKey at(BlockPos position) {
            return new SectorKey(Section.at(position), Section.quadrant(position));
        }

        static long packed(BlockPos position) {
            return packed(position.getX(), position.getY(), position.getZ());
        }

        static long packed(int x, int y, int z) {
            return ChunkPos.asLong(x >> 4, z >> 4) << 3 | (y >> 5);
        }
    }

    public static final class Effect {
        private final Session session;
        private final WorldIndex index;
        private final long id;
        private final int dimension;
        private final EntityPlayerMP owner;
        private final NetHandlerPlayServer ownerConnection;
        private Status status = new Status(0, 0, 0);
        private boolean hudQueued;
        private final ConstructionArea.Builder areaBuilder = new ConstructionArea.Builder();
        private final Region[] regions = new Region[ConstructionEffectPayload.REGION_COUNT];
        private final Long2ObjectLinkedOpenHashMap<Sector> sectors = new Long2ObjectLinkedOpenHashMap<>(4);
        private final Reference2ObjectOpenHashMap<EntityPlayerMP, Audience> audience = new Reference2ObjectOpenHashMap<>();
        private @Nullable ConstructionChunkPlan.Builder chunkPlanBuilder;
        private @Nullable ConstructionChunkPlan chunkPlan;
        private @Nullable ConstructionFadePlan fadePlan;
        private @Nullable Layout layout;
        private long unsettled;
        private boolean publishing;
        private boolean dismantle;
        private long startTick = -1;
        private int cellCount;
        private int wireTicks;
        private long settled;
        private @Nullable End ending;
        private boolean endReady;
        private boolean retiring;
        private int pendingDeltas;

        private Effect(Session session, WorldIndex index, EntityPlayerMP owner) {
            this.owner = owner;
            ownerConnection = owner.connection;
            this.session = session;
            this.index = index;
            id = session.nextId = Math.incrementExact(session.nextId);
            dimension = index.world.provider.getDimension();
        }

        private static boolean inBounds(BlockPos pos) {
            return pos.getY() >= 0 && pos.getY() < 256 && pos.getX() >= -30000000 && pos.getX() < 30000000
                && pos.getZ() >= -30000000 && pos.getZ() < 30000000;
        }

        public void includeInArea(BlockPos position) {
            if (startTick >= 0) throw new IllegalStateException("Cannot change a published construction area");
            areaBuilder.include(position.getX(), position.getY(), position.getZ());
        }

        public boolean prepareChunkPlanStep(LongArrayList positions) {
            if (startTick >= 0) throw new IllegalStateException("Cannot prepare a published construction plan");
            if (chunkPlan != null) return true;
            var builder = chunkPlanBuilder;
            if (builder == null) chunkPlanBuilder = builder = new ConstructionChunkPlan.Builder(positions);
            if (!builder.step()) return false;
            chunkPlan = builder.result();
            chunkPlanBuilder = null;
            return true;
        }

        public void planFades(int[] workByGroup) {
            fadePlan = ConstructionFadePlan.create(workByGroup);
        }

        public void add(BlockPos position, boolean fluid, int physicalGroup) {
            int group = Objects.requireNonNull(fadePlan).group(physicalGroup);
            long key = SectorKey.packed(position);
            var sector = sectors.get(key);
            if (sector == null) {
                sector = new Sector(this, SectorKey.at(position));
                sectors.put(key, sector);
            }
            int cell = Section.index(position);
            sector.add(cell, fluid);
            sector.assign(cell, group);
            cellCount++;
            int stage = regionIndex(group, fluid);
            var region = regions[stage];
            if (region == null) regions[stage] = region = new Region(stage);
            region.remaining++;
            long bit = 1L << stage;
            if ((sector.regionMask & bit) == 0) {
                sector.regionMask |= bit;
                region.sectors.add(sector);
            }
        }

        private int regionIndex(int group, boolean fluid) {
            return ConstructionEffectPayload.region(group, fluid && Objects.requireNonNull(fadePlan).size() > 1);
        }

        public ConstructionChunkPlan chunkPlan() {
            return Objects.requireNonNull(chunkPlan, "Construction chunks have not been prepared");
        }

        public void start(final boolean dismantle) {
            this.dismantle = dismantle;
            if (startTick >= 0 || !live()) throw new IllegalStateException("Cannot start this construction projection");
            var plan = Objects.requireNonNull(fadePlan);
            startTick = currentTick();
            wireTicks = ConstructionEffectPayload.wireTicks(cellCount);
            chunkPlan = null;
            if (!sectors.isEmpty()) {
                layout = new Layout(dismantle, startTick, wireTicks, plan.size(), areaBuilder.build());
                queueHud();
                publishing = true;
                session.work.enqueue(new PublishEffect(this, sectors.values().iterator()));
            }
        }

        private boolean introComplete() {
            return cellCount == 0 || currentTick() - startTick >= (dismantle ? 0 : wireTicks) + ConstructionEffectPayload.WHITE_TRANSITION_TICKS;
        }

        public boolean ready() {
            return startTick >= 0 && ending == null && live() && introComplete();
        }

        public boolean resolved(BlockPos position, boolean fluid, boolean changed) {
            if (!inBounds(position)) return false;
            var sector = sectors.get(SectorKey.packed(position));
            if (sector == null) return false;
            int cell = Section.index(position);
            if (!sector.cells.get(cell)) return false;
            int index = regionIndex(sector.group(cell), fluid);
            var region = Objects.requireNonNull(regions[index]);
            if (region.remaining <= 0)
                throw new IllegalStateException("Construction group already resolved at " + position);
            if (!changed) {
                sector.exclude(cell);
                sector.queueDelta();
            } else if (this.index.lighting != null) {
                sector.writtenRegions |= 1L << index;
            }
            if (--region.remaining != 0) return false;
            unsettled |= 1L << index;
            return true;
        }

        /**
         * Called after this task's lighting and neighbor notifications have been processed.
         */
        public void flushSettled() {
            long tick = currentTick();
            for (long mask = unsettled; mask != 0; mask &= mask - 1) {
                var region = regions[Long.numberOfTrailingZeros(mask)];
                region.syncAfterTick = tick;
                for (int index = 0; index < region.sectors.size(); index++) region.sectors.get(index).queueDelta();
            }
            unsettled = 0;
        }

        private long currentTick() {
            return Integer.toUnsignedLong(session.server.getTickCounter());
        }

        private boolean withinRange(EntityPlayerMP player) {
            var layout = this.layout;
            return layout != null && layout.area().withinRenderDistance(player.posX, player.posY, player.posZ);
        }

        public void status(int missing, int errors, int unloaded) {
            if (status.missing() == missing && status.errors() == errors && status.unloaded() == unloaded) return;
            status = new Status(missing, errors, unloaded);
            if (startTick >= 0) queueHud();
        }

        private void queueHud() {
            var info = layout;
            if (hudQueued || info == null) return;
            hudQueued = true;
            session.work.enqueue(() -> {
                var end = ending;
                if (live() && (end == null || !end.cancelled()) && connected(owner, ownerConnection)
                    && PktConstructionEffect.supportedBy(ownerConnection.netManager)) {
                    var recipient = recipient(owner);
                    if (!status.equals(recipient.lastStatus)) {
                        var message = new Hud(dimension, id, recipient.known ? null : info, status);
                        if (!session.send(owner, message)) return WorkResult.WAIT;
                        recipient.known = true;
                        recipient.lastStatus = status;
                    }
                }
                hudQueued = false;
                return WorkResult.DONE;
            });
        }

        public void finish(boolean successful) {
            end(false, successful);
        }

        public void cancel() {
            end(true, false);
        }

        private void end(boolean cancelled, boolean successful) {
            if (startTick < 0 || sectors.isEmpty() || ending != null || !live()) return;
            ending = new End(dimension, id, currentTick(), cancelled, successful);
            if (!cancelled) queueHud();
            session.work.enqueue(new CloseEffect(this));
        }

        private boolean live() {
            return current == session && session.worlds.get(index.world) == index;
        }

        private boolean connected(EntityPlayerMP player, NetHandlerPlayServer connection) {
            return player.world == index.world && player.connection == connection && connection.netManager.isChannelOpen()
                && player.server.getPlayerList().getPlayerByUUID(player.getUniqueID()) == player;
        }

        private Audience recipient(EntityPlayerMP player) {
            var recipient = audience.get(player);
            if (recipient != null && recipient.connection == player.connection) return recipient;
            if (recipient != null) {
                session.audiences.remove(recipient);
                recipient.detach();
            }
            recipient = new Audience(this, player);
            audience.put(player, recipient);
            session.audiences.add(recipient);
            return recipient;
        }

        private boolean deliverEnd(Audience recipient, End end) {
            if (!recipient.known || recipient.endSent || audience.get(recipient.player) != recipient
                || !connected(recipient.player, recipient.connection)) return true;
            if (!session.send(recipient.player, end)) return false;
            recipient.endSent = true;
            return true;
        }

        private boolean cancelled() {
            var end = ending;
            return end != null && end.cancelled();
        }

        private boolean replayExpired() {
            var end = endReady ? ending : null;
            return end != null && session.tick - end.serverTick() >= (dismantle ? ConstructionEffectPayload.FINAL_WIRE_TICKS : ConstructionEffectPayload.REGION_FADE_TICKS);
        }
    }

    private record PublishEffect(Effect effect, Iterator<Sector> sectors) implements Work {
        @Override
        public WorkResult advance() {
            if (effect.live() && !effect.cancelled()) {
                sectors.next().attach();
                if (sectors.hasNext()) return WorkResult.CONTINUE;
            }
            effect.publishing = false;
            return WorkResult.DONE;
        }
    }

    private static final class CloseEffect implements Work {
        private final Effect effect;
        private @Nullable ObjectArrayList<Audience> endingAudience;
        private @Nullable Iterator<Sector> retiringSectors;
        private int cursor;
        private long retireAfter;

        CloseEffect(Effect effect) {
            this.effect = effect;
        }

        @Override
        public WorkResult advance() {
            if (!effect.live()) return WorkResult.DONE;
            var end = effect.ending;
            if (end == null) throw new IllegalStateException("Construction effect has no terminal state");
            var players = endingAudience;
            if (players == null) {
                if (!end.cancelled() && (effect.publishing || effect.pendingDeltas != 0 || effect.unsettled != 0 || effect.hudQueued))
                    return WorkResult.WAIT;
                players = new ObjectArrayList<>(effect.audience.values());
                endingAudience = players;
                end = effect.ending = new End(end.dimension(), end.taskId(), effect.session.tick, end.cancelled(), end.successful());
                effect.endReady = true;
                retireAfter = end.serverTick() + (end.cancelled() ? 0 : ConstructionEffectPayload.FINAL_WIRE_TICKS);
            }
            if (cursor < players.size()) {
                if (!effect.deliverEnd(players.get(cursor), end)) return WorkResult.WAIT;
                cursor++;
                return WorkResult.CONTINUE;
            }
            if (effect.session.tick < retireAfter) return WorkResult.WAIT;
            var sectors = retiringSectors;
            if (sectors == null) {
                effect.retiring = true;
                sectors = retiringSectors = effect.sectors.values().iterator();
            }
            if (!sectors.hasNext()) {
                effect.session.audiences.removeAll(effect.audience.values());
                effect.audience.clear();
                return WorkResult.DONE;
            }
            sectors.next().detach();
            return WorkResult.CONTINUE;
        }
    }

    private static final class Sector {
        private final Effect effect;
        private final SectorKey key;
        private final long chunk;
        private final BitSet cells = new BitSet();
        private BitSet fluids = NO_CELLS;
        private @Nullable BitSet excluded;
        private @Nullable BitSet pendingExclude;
        private @Nullable BitSet visibleCells;
        private @Nullable BitSet visibleFluids;
        private @Nullable byte[] visibleGroups;
        private @Nullable Geometry snapshotWithLayout;
        private @Nullable Geometry snapshotWithoutLayout;
        private long snapshotSettled;
        private byte firstGroup = -1;
        private @Nullable Short2ByteOpenHashMap otherGroups;
        private @Nullable Reference2ObjectOpenHashMap<EntityPlayerMP, Viewer> viewers;
        private long regionMask;
        private long writtenRegions;
        private long sentSettlement;
        private int revision;
        private boolean deltaQueued;

        Sector(Effect effect, SectorKey key) {
            this.effect = effect;
            this.key = key;
            chunk = ChunkPos.asLong(key.section.chunkX(key.quadrant), key.section.chunkZ(key.quadrant));
        }

        void attach() {
            var index = effect.index;
            index.chunks.computeIfAbsent(chunk, _ -> new ReferenceOpenHashSet<>(2)).add(this);
            var chunkMap = index.world.getPlayerChunkMap();
            int x = key.section.chunkX(key.quadrant);
            int z = key.section.chunkZ(key.quadrant);
            var entry = chunkMap.getEntry(x, z);
            if (entry == null) return;
            for (var player : entry.getWatchingPlayers()) {
                // Pulsar can defer the initial packet for a player already in the entry.
                if (index.lighting == null || chunkMap.isPlayerWatchingChunk(player, x, z)) watch(player);
            }
        }

        void detach() {
            var chunks = effect.index.chunks;
            var siblings = chunks.get(chunk);
            if (siblings != null && siblings.remove(this) && siblings.isEmpty()) chunks.remove(chunk);
            viewers = null;
        }

        void watch(EntityPlayerMP player) {
            if (effect.retiring || effect.cancelled() || effect.replayExpired()
                || !PktConstructionEffect.supportedBy(player.connection.netManager)) return;
            var viewers = this.viewers;
            if (viewers != null) {
                var previous = viewers.get(player);
                if (previous != null && previous.recipient.connection == player.connection) return;
            }
            var recipient = effect.recipient(player);
            viewers = this.viewers; // Replacing a connection detaches the old recipient from all its sectors.
            if (viewers == null) this.viewers = viewers = new Reference2ObjectOpenHashMap<>(2);
            var view = new Viewer(this, recipient);
            viewers.put(player, view);
            recipient.viewers.add(view);
            view.queueReplay();
        }

        void unwatch(EntityPlayerMP player) {
            var viewers = this.viewers;
            if (viewers == null) return;
            var viewer = viewers.remove(player);
            if (viewer == null) return;
            if (viewers.isEmpty()) this.viewers = null;
            var recipient = viewer.recipient;
            recipient.viewers.remove(viewer);
            if (viewer.revision >= 0) recipient.sentSectors--;
            recipient.discardIfUnused();
        }

        boolean watching(Viewer view) {
            var viewers = this.viewers;
            return viewers != null && view.recipient.near && viewers.get(view.recipient.player) == view;
        }

        void add(int cell, boolean fluid) {
            cells.set(cell);
            if (fluid) {
                if (fluids == NO_CELLS) fluids = new BitSet();
                fluids.set(cell);
            }
        }

        void assign(int cell, int group) {
            if (firstGroup < 0) firstGroup = (byte) group;
            else if (group != firstGroup) {
                var others = otherGroups;
                if (others == null) {
                    otherGroups = others = new Short2ByteOpenHashMap();
                    others.defaultReturnValue(firstGroup);
                }
                others.put((short) cell, (byte) group);
            }
        }

        int group(int cell) {
            var others = otherGroups;
            return others == null ? firstGroup : others.get((short) cell);
        }

        void exclude(int cell) {
            if (pendingExclude == null) pendingExclude = new BitSet();
            pendingExclude.set(cell);
        }

        private void queueDelta() {
            if (deltaQueued) return;
            deltaQueued = true;
            effect.pendingDeltas++;
            effect.session.work.enqueue(new Delta(this));
        }

        @Nullable Geometry snapshot(Audience recipient, boolean replacing) {
            var visible = visibleCells;
            if (visible == null) {
                visible = cells;
                var liquids = fluids;
                var removed = excluded;
                if (removed != null) {
                    visible = (BitSet) cells.clone();
                    visible.andNot(removed);
                    if (fluids.intersects(removed)) {
                        liquids = (BitSet) fluids.clone();
                        liquids.andNot(removed);
                    }
                }
                visibleCells = visible;
                visibleFluids = liquids;
                visibleGroups = visible.isEmpty() ? ConstructionEffectPayload.NO_GROUPS
                    : otherGroups == null ? new byte[]{firstGroup} : ConstructionEffectPayload.packGroups(visible, this::group);
            }
            if (visible.isEmpty() && !replacing) return null;
            long times = regionMask & effect.settled;
            if (snapshotSettled != times) {
                snapshotWithLayout = null;
                snapshotWithoutLayout = null;
                snapshotSettled = times;
            }
            var cached = recipient.known ? snapshotWithoutLayout : snapshotWithLayout;
            if (cached != null && cached.serverTick() == effect.session.tick) return cached;
            var update = new Geometry(effect.dimension, effect.id, effect.session.tick, key.section, key.quadrant, true,
                recipient.known ? null : Objects.requireNonNull(effect.layout), visible, visibleFluids,
                visibleGroups, NO_CELLS, regionTimes(times));
            if (recipient.known) snapshotWithoutLayout = update;
            else snapshotWithLayout = update;
            return update;
        }

        void invalidateGeometry() {
            visibleCells = null;
            visibleFluids = null;
            visibleGroups = null;
            snapshotWithLayout = null;
            snapshotWithoutLayout = null;
        }

        Geometry delta(BitSet removed, List<RegionTime> times) {
            return new Geometry(effect.dimension, effect.id, effect.session.tick, key.section, key.quadrant, false, null,
                NO_CELLS, NO_CELLS, ConstructionEffectPayload.NO_GROUPS, removed, times);
        }

        private boolean hasMissingTimes(Audience recipient) {
            return (regionMask & effect.settled & ~recipient.settled) != 0;
        }

        private List<RegionTime> missingTimes(Audience recipient) {
            return regionTimes(regionMask & effect.settled & ~recipient.settled);
        }

        private List<RegionTime> regionTimes(long mask) {
            if (mask == 0) return List.of();
            var times = new ObjectArrayList<RegionTime>(Long.bitCount(mask));
            for (long remaining = mask; remaining != 0; remaining &= remaining - 1) {
                int i = Long.numberOfTrailingZeros(remaining);
                var region = Objects.requireNonNull(effect.regions[i]);
                times.add(Objects.requireNonNull(region.time));
            }
            return times;
        }
    }

    private static final class Region {
        final ObjectArrayList<Sector> sectors = new ObjectArrayList<>();
        final int index;
        int remaining;
        long syncAfterTick = -1;
        private int lightCursor;
        private long retryAfterTick = -1;
        private long lightReadyTick = -1;
        private @Nullable RegionTime time;

        Region(int index) {
            this.index = index;
        }

        boolean canSettle(Effect effect) {
            long tick = effect.session.tick;
            if (tick <= syncAfterTick) return false;
            var lighting = effect.index.lighting;
            if (lighting == null || tick - syncAfterTick >= MAX_LIGHT_WAIT_TICKS) return true;
            if (lightReadyTick >= 0) return tick > lightReadyTick;
            if (tick <= retryAfterTick) return false;
            var sector = sectors.get(lightCursor);
            if ((sector.writtenRegions & 1L << index) != 0 && !lighting.canSend(sector.chunk)) {
                retryAfterTick = tick;
                return false;
            }
            // Each work step checks at most one sector. Give the server another tick to dispatch deferred packets.
            if (++lightCursor == sectors.size()) lightReadyTick = tick;
            return false;
        }

        void settle(long tick) {
            syncAfterTick = -1;
            time = new RegionTime(index, tick);
        }
    }

    private static final class Viewer implements Work {
        final Sector sector;
        final Audience recipient;
        int revision = -1;
        boolean replayQueued;

        Viewer(Sector sector, Audience recipient) {
            this.sector = sector;
            this.recipient = recipient;
        }

        void queueReplay() {
            if (replayQueued || !recipient.near || sector.effect.replayExpired()
                || revision == sector.revision && !sector.hasMissingTimes(recipient)) return;
            replayQueued = true;
            recipient.pendingReplays++;
            recipient.effect.session.work.enqueue(this);
        }

        @Override
        public WorkResult advance() {
            var effect = recipient.effect;
            if (!effect.live() || effect.retiring || effect.cancelled() || effect.replayExpired() || !sector.watching(this))
                return done();
            var end = effect.endReady ? effect.ending : null;
            var after = recipient.endSent ? null : end;
            if (effect.session.remainingPackets < (after == null ? 1 : 2)) return WorkResult.WAIT;
            Geometry update;
            if (revision == sector.revision) {
                var times = sector.missingTimes(recipient);
                if (times.isEmpty()) return done();
                update = sector.delta(NO_CELLS, times);
            } else {
                update = sector.snapshot(recipient, revision >= 0);
                if (update == null) return done();
            }
            if (!effect.session.send(recipient.player, update, after)) return WorkResult.WAIT;
            recipient.recordTimes(update);
            if (revision < 0) recipient.sentSectors++;
            revision = sector.revision;
            recipient.known = true;
            if (after != null) recipient.endSent = true;
            return done();
        }

        private WorkResult done() {
            replayQueued = false;
            recipient.pendingReplays--;
            return WorkResult.DONE;
        }
    }

    private static final class Audience {
        final Effect effect;
        final EntityPlayerMP player;
        final NetHandlerPlayServer connection;
        final ReferenceOpenHashSet<Viewer> viewers = new ReferenceOpenHashSet<>(4);
        long settled;
        int sentSectors;
        int pendingReplays;
        boolean near;
        boolean hidden;
        boolean visibilityQueued;
        boolean known;
        boolean endSent;
        @Nullable Status lastStatus;

        Audience(Effect effect, EntityPlayerMP player) {
            this.effect = effect;
            this.player = player;
            connection = player.connection;
            near = effect.withinRange(player);
        }

        void recordTimes(Geometry packet) {
            for (var time : packet.regions()) settled |= 1L << time.region();
        }

        void updateDistance() {
            boolean withinRange = effect.withinRange(player);
            if (near == withinRange) return;
            near = withinRange;
            if (near) for (var viewer : viewers) viewer.queueReplay();
            if (!visibilityQueued && (near ? hidden : !hidden && sentSectors != 0)) {
                visibilityQueued = true;
                effect.session.work.enqueue(this::syncVisibility);
            }
        }

        private WorkResult syncVisibility() {
            if (effect.live() && !effect.cancelled() && !effect.retiring && effect.audience.get(player) == this
                && (near ? hidden && !effect.replayExpired() : !hidden && sentSectors != 0)) {
                if (near && pendingReplays != 0) return WorkResult.WAIT;
                if (!effect.session.send(player, new Visibility(effect.dimension, effect.id, near)))
                    return WorkResult.WAIT;
                hidden = !near;
            }
            visibilityQueued = false;
            return WorkResult.DONE;
        }

        void discardIfUnused() {
            if (known || !viewers.isEmpty()) return;
            effect.session.audiences.remove(this);
            effect.audience.remove(player, this);
        }

        void detach() {
            for (var viewer : viewers) {
                var watching = viewer.sector.viewers;
                if (watching != null && watching.remove(player, viewer) && watching.isEmpty())
                    viewer.sector.viewers = null;
            }
            viewers.clear();
            effect.audience.remove(player, this);
        }
    }

    private static final class Delta implements Work {
        private final Sector sector;
        private BitSet removed = NO_CELLS;
        private @Nullable Geometry lastDelta;
        private long lastMissing;
        private @Nullable ObjectArrayList<Viewer> audience;
        private int cursor;
        private int baseRevision;
        private long settled;

        Delta(Sector sector) {
            this.sector = sector;
        }

        @Override
        public WorkResult advance() {
            var effect = sector.effect;
            if (!effect.live() || effect.cancelled()) return finish();
            var players = audience;
            if (players == null) {
                boolean waiting = false;
                for (long mask = sector.regionMask; mask != 0; mask &= mask - 1) {
                    int index = Long.numberOfTrailingZeros(mask);
                    var region = Objects.requireNonNull(effect.regions[index]);
                    if (region.syncAfterTick >= 0 && region.time == null) {
                        if (region.canSettle(effect)) {
                            region.settle(effect.session.tick);
                            effect.settled |= 1L << index;
                        } else waiting = true;
                    }
                }
                settled = 0;
                for (long mask = sector.regionMask; mask != 0; mask &= mask - 1) {
                    int index = Long.numberOfTrailingZeros(mask);
                    long bit = 1L << index;
                    var region = Objects.requireNonNull(effect.regions[index]);
                    if (region.time != null && (sector.sentSettlement & bit) == 0) settled |= bit;
                }
                var pending = sector.pendingExclude;
                if (settled == 0 && pending == null) return waiting ? WorkResult.WAIT : finish();
                baseRevision = sector.revision;
                removed = pending == null ? NO_CELLS : pending;
                sector.pendingExclude = null;
                if (pending != null) {
                    sector.revision++;
                    var excluded = sector.excluded;
                    if (excluded == null) sector.excluded = (BitSet) pending.clone();
                    else excluded.or(pending);
                    sector.invalidateGeometry();
                }
                var viewers = sector.viewers;
                if (viewers == null || viewers.isEmpty()) {
                    sector.sentSettlement |= settled;
                    return waiting ? WorkResult.WAIT : finish();
                }
                players = new ObjectArrayList<>(viewers.values());
                audience = players;
                cursor = 0;
            }
            if (cursor < players.size()) {
                var viewer = players.get(cursor);
                if (viewer.revision >= 0 && sector.watching(viewer)) {
                    if (viewer.revision < baseRevision || viewer.replayQueued) viewer.queueReplay();
                    else {
                        var recipient = viewer.recipient;
                        long missing = settled & ~recipient.settled;
                        var removedForViewer = viewer.revision == sector.revision ? NO_CELLS : removed;
                        if (missing != 0 || !removedForViewer.isEmpty()) {
                            var message = delta(missing, removedForViewer);
                            if (!effect.session.send(recipient.player, message)) return WorkResult.WAIT;
                            recipient.recordTimes(message);
                        }
                        viewer.revision = sector.revision;
                    }
                }
                cursor++;
                return WorkResult.CONTINUE;
            }
            sector.sentSettlement |= settled;
            audience = null;
            lastDelta = null;
            removed = NO_CELLS;
            return WorkResult.CONTINUE;
        }

        private Geometry delta(long missing, BitSet removedForViewer) {
            var message = lastDelta;
            if (message != null && lastMissing == missing && message.excluded() == removedForViewer
                && message.serverTick() == sector.effect.session.tick) return message;
            lastMissing = missing;
            return lastDelta = sector.delta(removedForViewer, sector.regionTimes(missing));
        }

        private WorkResult finish() {
            sector.deltaQueued = false;
            sector.effect.pendingDeltas--;
            return WorkResult.DONE;
        }
    }
}
