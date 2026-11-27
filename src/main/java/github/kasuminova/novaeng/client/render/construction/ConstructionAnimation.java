package github.kasuminova.novaeng.client.render.construction;

import github.kasuminova.novaeng.common.assembly.effect.ConstructionArea;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.Geometry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.renderer.culling.ClippingHelper;

import java.util.Arrays;
import java.util.BitSet;

import static github.kasuminova.novaeng.client.render.construction.ConstructionSurface.FLUID;
import static github.kasuminova.novaeng.client.render.construction.ConstructionSurface.HEIGHT;
import static github.kasuminova.novaeng.client.render.construction.ConstructionSurface.SOLID;
import static github.kasuminova.novaeng.client.render.construction.ConstructionSurface.WIDTH;
import static github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.FINAL_WIRE_TICKS;
import static github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.LAYERS_PER_SECTION;
import static github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.MAX_LOGICAL_CHUNKS;
import static github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.REGION_FADE_TICKS;
import static github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.WHITE_TRANSITION_TICKS;

final class ConstructionAnimation {
    static final int SLICE_CELLS = WIDTH * HEIGHT * WIDTH;
    static final double MIN_OPACITY = .004;
    private static final long UNSET = Long.MIN_VALUE;

    final boolean dismantle;
    boolean owner;
    boolean complete;
    int missing, errors, unloaded;
    String hudText;
    final ConstructionArea area;
    final ConstructionMeshes.Arena arena;
    final Long2ObjectOpenHashMap<SectionMesh> sections = new Long2ObjectOpenHashMap<>(4);
    final double[] opacities = new double[MAX_LOGICAL_CHUNKS * 2];
    private final long[] settledAt = new long[MAX_LOGICAL_CHUNKS * 2];
    private final long startTick;
    private final int wireTicks;
    private final int groupCount;
    boolean wireReleased;
    boolean hidden;
    private long firstServerTick = UNSET;
    private long latestServerTick = UNSET;
    private long introAt = UNSET;
    private long finishAt = UNSET;
    private long pendingReleases;
    private long expiredRegions;

    ConstructionAnimation(final boolean dismantle, final long startTick, final int wireTicks, final int groupCount, final ConstructionArea area) {
        this.dismantle = dismantle;
        this.startTick = startTick;
        this.wireTicks = wireTicks;
        this.groupCount = groupCount;
        this.area = area;
        arena = new ConstructionMeshes.Arena(area.min());
        Arrays.fill(settledAt, UNSET);
    }

    private static double smooth(final double age, final int duration) {
        final double value = Math.clamp(age / duration, 0, 1);
        return value * value * (3 - 2 * value);
    }

    private static long key(final int x, final int y, final int z) {
        return ((long) x & 0x3ffffff) << 38 | ((long) z & 0x3ffffff) << 12 | ((long) y & 0xfff);
    }

    private static int neighbors(final int cell) {
        final int dx = boundary(cell & 15, WIDTH);
        final int dy = boundary(cell >> 8, HEIGHT);
        final int dz = boundary(cell >> 4 & 15, WIDTH);
        if ((dx | dy | dz) == 0) return 0;
        int result = 0;
        for (int a = Math.min(dx, 0); a <= Math.max(dx, 0); a++) {
            for (int b = Math.min(dy, 0); b <= Math.max(dy, 0); b++) {
                for (int c = Math.min(dz, 0); c <= Math.max(dz, 0); c++)
                    result |= 1 << ((b + 1) * 9 + (c + 1) * 3 + a + 1);
            }
        }
        return result & ~(1 << 13);
    }

    private static int boundary(final int value, final int limit) {
        if (value == 0) return -1;
        return value == limit - 1 ? 1 : 0;
    }

    void finish(final long serverTick, final long clientTick) {
        if (finishAt == UNSET)
            finishAt = firstServerTick > serverTick ? clientTick - (latestServerTick - serverTick) : clientTick;
    }

    void metrics(final int missing, final int errors, final int unloaded) {
        if (this.missing != missing || this.errors != errors || this.unloaded != unloaded) hudText = null;
        this.missing = missing;
        this.errors = errors;
        this.unloaded = unloaded;
    }

    private double finishAge(final double clientTick) {
        return finishAt == UNSET ? -1 : Math.max(0, clientTick - finishAt);
    }

    double wireOpacity(final double now) {
        if (dismantle) {
            final double after = finishAge(now);
            return after < 0 || wireReleased ? 0 : .92 * smooth(after, WHITE_TRANSITION_TICKS)
                * (1 - smooth(after - WHITE_TRANSITION_TICKS, FINAL_WIRE_TICKS - WHITE_TRANSITION_TICKS));
        }
        return finishAt != UNSET || introAt == UNSET || wireReleased ? 0 : .92 * (1 - smooth(now - introAt - wireTicks, WHITE_TRANSITION_TICKS));
    }

    boolean prepareOutline(final double now) {
        return dismantle && finishAge(now) < WHITE_TRANSITION_TICKS;
    }

    boolean prepareShell(final double now) {
        return !dismantle && introAt != UNSET && finishAt == UNSET && now <= introAt + wireTicks;
    }

    void sampleOpacity(final double now) {
        final double white = introAt == UNSET ? 0 : smooth(now - introAt - (dismantle ? 0 : wireTicks), WHITE_TRANSITION_TICKS);
        for (int group = 0; group < groupCount; group++) {
            opacities[group] = opacity(group, white, now);
            final int fluid = group + MAX_LOGICAL_CHUNKS;
            opacities[fluid] = opacity(fluid, white, now);
        }
    }

    private double opacity(final int region, final double white, final double now) {
        final long settled = settledAt[region];
        if (dismantle && finishAt != UNSET && settled == UNSET) return 0;
        return settled == UNSET ? white : white * (1 - smooth(now - settled, REGION_FADE_TICKS));
    }

    boolean expired(final long now) {
        return finishAge(now) >= (owner ? 60 : FINAL_WIRE_TICKS);
    }

    void geometry(final Geometry packet, final long clientTick) {
        if (firstServerTick == UNSET) firstServerTick = packet.serverTick();
        latestServerTick = Math.max(latestServerTick, packet.serverTick());
        if (packet.snapshot()) {
            final long startedAt = clientTick - (packet.serverTick() - startTick);
            introAt = introAt == UNSET ? startedAt : Math.min(introAt, startedAt);
        }
        final var section = packet.section();
        final int x = section.chunkX(packet.quadrant());
        final int y = section.y() * LAYERS_PER_SECTION;
        final int z = section.chunkZ(packet.quadrant());
        if (packet.snapshot()) {
            final byte[] groups = packet.groups();
            replace(x, y, z, packet.cells(), packet.fluids(), groups.length <= 1 ? groups : packet.cellGroups());
        } else if (!packet.excluded().isEmpty()) {
            exclude(x, y, z, packet.excluded());
        }
        final var regionTimes = packet.regions();
        for (int index = 0; index < regionTimes.size(); index++) {
            final var time = regionTimes.get(index);
            final int region = time.region();
            if (region % MAX_LOGICAL_CHUNKS >= groupCount) {
                throw new IllegalArgumentException("Construction transition references an unknown group");
            }
            final long at = clientTick - (packet.serverTick() - time.settledTick());
            settle(region, finishAt == UNSET ? at : Math.min(at, finishAt));
        }
    }

    private void settle(final int region, final long tick) {
        if (groupCount == 1) {
            settleRegion(0, tick);
            settleRegion(MAX_LOGICAL_CHUNKS, tick);
        } else settleRegion(region, tick);
    }

    private void settleRegion(final int region, final long tick) {
        if (settledAt[region] != UNSET && settledAt[region] <= tick) return;
        settledAt[region] = tick;
        pendingReleases |= 1L << region;
    }

    private void replace(final int x, final int y, final int z, final BitSet cells, final BitSet fluids, final byte[] groups) {
        final int uniformGroup = groups.length == 1 ? Byte.toUnsignedInt(groups[0]) : -1;
        int next = cells.nextSetBit(0);
        int ordinal = 0;
        for (int layer = 0; layer < LAYERS_PER_SECTION; layer++) {
            final int from = layer * SLICE_CELLS;
            final int end = from + SLICE_CELLS;
            final int height = y + layer;
            final long key = key(x, height, z);
            var mesh = sections.get(key);
            if (next < 0 || next >= end) {
                if (mesh != null) remove(mesh, key);
                continue;
            }
            if (mesh == null) {
                mesh = new SectionMesh(this, x, height, z);
                sections.put(key, mesh);
            }
            final byte[] materials = mesh.cells;
            long present = 0;
            int occupied = 0;
            int neighbors = 0;
            boolean changed = false;
            for (int index = 0; index < SLICE_CELLS; index++) {
                byte material = 0;
                if (from + index == next) {
                    final int group = uniformGroup < 0 ? Byte.toUnsignedInt(groups[ordinal++]) : uniformGroup;
                    if (group >= groupCount)
                        throw new IllegalArgumentException("Construction snapshot references an unknown group");
                    final boolean fluid = fluids.get(next);
                    material = ConstructionSurface.groupMaterial(group, fluid ? FLUID : SOLID);
                    present |= 1L << (fluid ? group + MAX_LOGICAL_CHUNKS : group);
                    occupied++;
                    next = cells.nextSetBit(next + 1);
                }
                if (materials[index] != material) {
                    materials[index] = material;
                    neighbors |= neighbors(index);
                    changed = true;
                }
            }
            if (changed) {
                mesh.update(present & ~expiredRegions, occupied);
                invalidateNeighbors(x, height, z, neighbors);
            }
        }
    }

    private void exclude(final int x, final int y, final int z, final BitSet removed) {
        int next = removed.nextSetBit(0);
        while (next >= 0) {
            final int layer = next / SLICE_CELLS;
            final int height = y + layer;
            final int from = layer * SLICE_CELLS;
            final int end = from + SLICE_CELLS;
            final long key = key(x, height, z);
            final var mesh = sections.get(key);
            if (mesh == null) {
                next = removed.nextSetBit(end);
                continue;
            }
            int neighbors = 0;
            int count = 0;
            do {
                final int index = next - from;
                if (mesh.cells[index] != 0) {
                    mesh.cells[index] = 0;
                    neighbors |= neighbors(index);
                    count++;
                }
                next = removed.nextSetBit(next + 1);
            } while (next >= 0 && next < end);
            if (count == 0) continue;
            final int occupied = mesh.occupied - count;
            if (occupied == 0) {
                sections.remove(key);
                mesh.dispose();
            } else {
                long present = 0;
                for (final byte material : mesh.cells) {
                    if (material == 0) continue;
                    final int group = Byte.toUnsignedInt(material) >>> 2;
                    present |= 1L << ((material & 3) == FLUID ? group + MAX_LOGICAL_CHUNKS : group);
                }
                mesh.update(present & ~expiredRegions, occupied);
            }
            invalidateNeighbors(x, height, z, neighbors);
        }
    }

    private void remove(final SectionMesh mesh, final long key) {
        sections.remove(key);
        int neighbors = 0;
        for (int index = 0; index < SLICE_CELLS; index++) if (mesh.cells[index] != 0) neighbors |= neighbors(index);
        mesh.dispose();
        invalidateNeighbors(mesh.x, mesh.y, mesh.z, neighbors);
    }

    void forgetChunk(final int x, final int z) {
        final var min = area.min();
        final var max = area.max();
        if (sections.isEmpty() || x < (min.getX() >> 4) || x > (max.getX() >> 4) || z < (min.getZ() >> 4) || z > (max.getZ() >> 4))
            return;
        for (int y = min.getY() >> 3; y <= max.getY() >> 3; y++) {
            final long key = key(x, y, z);
            final var mesh = sections.get(key);
            if (mesh != null) remove(mesh, key);
        }
    }

    byte[] cells(final int x, final int y, final int z) {
        final var section = sections.get(key(x, y, z));
        return section == null ? null : section.cells;
    }

    private void invalidateNeighbors(final int x, final int y, final int z, int neighbors) {
        while (neighbors != 0) {
            final int neighbor = Integer.numberOfTrailingZeros(neighbors);
            neighbors &= neighbors - 1;
            final var adjacent = sections.get(key(x + neighbor % 3 - 1, y + neighbor / 9 - 1, z + neighbor / 3 % 3 - 1));
            if (adjacent != null) adjacent.invalidate();
        }
    }

    void releaseExpiredMeshes(final long now) {
        long releasing = 0;
        for (long remaining = pendingReleases; remaining != 0; remaining &= remaining - 1) {
            final int region = Long.numberOfTrailingZeros(remaining);
            if (now - settledAt[region] >= REGION_FADE_TICKS) releasing |= 1L << region;
        }
        final boolean releaseWire = !wireReleased && (dismantle ? finishAt != UNSET && now - finishAt >= FINAL_WIRE_TICKS
            : introAt != UNSET && now - introAt >= wireTicks + WHITE_TRANSITION_TICKS);
        if (!releaseWire && releasing == 0) return;
        pendingReleases &= ~releasing;
        expiredRegions |= releasing;
        wireReleased |= releaseWire;
        for (final var mesh : sections.values()) {
            if (releaseWire) mesh.wire.delete();
            mesh.retainRegions(mesh.present & ~releasing);
        }
    }

    void invalidateMeshes() {
        for (final var mesh : sections.values()) mesh.dispose();
    }

    void dispose() {
        for (final var mesh : sections.values()) mesh.dispose();
        arena.close();
    }

    static final class SectionMesh {
        final ConstructionAnimation task;
        final int x, y, z;
        final byte[] cells = new byte[SLICE_CELLS];
        final ConstructionMeshes.Part wire = new ConstructionMeshes.Part(ConstructionMeshes.WIRE);
        ConstructionMeshes.Part[] parts = ConstructionMeshes.NO_PARTS;
        long present;
        int occupied;
        int version;
        int seenFrame;
        boolean prepareWire;
        boolean prepareShell;

        SectionMesh(final ConstructionAnimation task, final int x, final int y, final int z) {
            this.task = task;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        boolean inFrustum(final ClippingHelper frustum, final double x, final double y, final double z) {
            final double minX = this.x * WIDTH - x;
            final double minY = this.y * HEIGHT - y;
            final double minZ = this.z * WIDTH - z;
            return frustum.isBoxInFrustum(minX, minY, minZ, minX + WIDTH, minY + HEIGHT, minZ + WIDTH);
        }

        void update(final long present, final int occupied) {
            this.occupied = occupied;
            retainRegions(present);
            invalidate();
        }

        void retainRegions(final long remaining) {
            if (present == remaining) return;
            final var next = remaining == 0 ? ConstructionMeshes.NO_PARTS : new ConstructionMeshes.Part[Long.bitCount(remaining)];
            long previous = present;
            int oldIndex = 0;
            int index = 0;
            for (long groups = remaining; groups != 0; groups &= groups - 1) {
                final int group = Long.numberOfTrailingZeros(groups);
                while (previous != 0 && Long.numberOfTrailingZeros(previous) < group) {
                    parts[oldIndex++].delete();
                    previous &= previous - 1;
                }
                if ((previous & 1L << group) != 0) {
                    next[index++] = parts[oldIndex++];
                    previous &= previous - 1;
                } else {
                    next[index++] = new ConstructionMeshes.Part((byte) group);
                }
            }
            while (oldIndex < parts.length) parts[oldIndex++].delete();
            present = remaining;
            parts = next;
        }

        void invalidate() {
            version++;
        }

        void dispose() {
            wire.delete();
            for (final var part : parts) part.delete();
            invalidate();
        }
    }
}
