package github.kasuminova.novaeng.common.assembly.effect;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * Geometry is a set of unit cells. The receiver emits their boundary vertices, omitting internal faces.
 */
public sealed interface ConstructionEffectPayload {
    int MAX_BYTES = 16 * 1024 - 1; // Forge prepends the message discriminator.
    byte[] NO_GROUPS = new byte[0];
    int WHITE_TRANSITION_TICKS = 3;
    int REGION_FADE_TICKS = 12;
    int FINAL_WIRE_TICKS = 20;
    int MIN_INITIAL_WIRE_TICKS = 6;
    int MAX_INITIAL_WIRE_TICKS = 28;
    int MAX_LOGICAL_CHUNKS = ConstructionChunkPlan.MAX_CHUNKS;
    int REGION_COUNT = MAX_LOGICAL_CHUNKS * 2;
    int CELL_COUNT = 32 * 32 * 32;
    int CHUNK_CELL_COUNT = CELL_COUNT / 4;
    int GROUP_BITS = 5;
    int PACKED_GROUPS = MAX_LOGICAL_CHUNKS;
    int GROUP_RUNS = PACKED_GROUPS + 1;
    int LAYER_HEIGHT = 8;
    int LAYERS_PER_SECTION = 32 / LAYER_HEIGHT;
    int HEADER_BYTES = 1 + Integer.BYTES + Long.BYTES;
    int LAYOUT_BYTES = Byte.BYTES + Long.BYTES + 2 * Byte.BYTES + 2 * Long.BYTES;

    static int wireTicks(int cells) {
        int log = Integer.SIZE - 1 - Integer.numberOfLeadingZeros(Math.max(1, cells));
        return Math.min(MAX_INITIAL_WIRE_TICKS, MIN_INITIAL_WIRE_TICKS + 2 * log);
    }

    static int region(int chunk, boolean fluid) {
        if (chunk < 0 || chunk >= MAX_LOGICAL_CHUNKS) throw new IllegalArgumentException("Invalid construction chunk");
        return chunk + (fluid ? MAX_LOGICAL_CHUNKS : 0);
    }

    private static int packedGroup(byte[] groups, int ordinal) {
        int bit = ordinal * GROUP_BITS;
        int offset = 1 + (bit >>> 3);
        int shift = bit & 7;
        int value = Byte.toUnsignedInt(groups[offset]);
        if (offset + 1 < groups.length) value |= Byte.toUnsignedInt(groups[offset + 1]) << 8;
        return value >>> shift & 31;
    }

    private static void setPackedGroup(byte[] groups, int cell, int group) {
        int bit = cell * GROUP_BITS;
        int offset = 1 + (bit >>> 3);
        int shift = bit & 7;
        int value = Byte.toUnsignedInt(groups[offset]);
        if (offset + 1 < groups.length) value |= Byte.toUnsignedInt(groups[offset + 1]) << 8;
        value |= group << shift;
        groups[offset] = (byte) value;
        if (offset + 1 < groups.length) groups[offset + 1] = (byte) (value >>> 8);
    }

    static byte[] packGroups(BitSet cells, IntUnaryOperator groupAtCell) {
        if (cells.isEmpty()) return NO_GROUPS;
        int packedLength = 1 + groupBytes(cells.cardinality());
        int first = cells.nextSetBit(0);
        int previous = groupAtCell.applyAsInt(first), length = 1;
        ByteArrayList runs = null;
        for (int cell = cells.nextSetBit(first + 1); cell >= 0; cell = cells.nextSetBit(cell + 1)) {
            int id = groupAtCell.applyAsInt(cell);
            if (id == previous) {
                length++;
                continue;
            }
            if (runs == null) {
                runs = new ByteArrayList();
                runs.add((byte) GROUP_RUNS);
            }
            appendRun(runs, previous, length);
            if (runs.size() >= packedLength) break;
            previous = id;
            length = 1;
        }
        if (runs == null) return new byte[]{(byte) previous};
        if (runs.size() < packedLength) {
            appendRun(runs, previous, length);
            if (runs.size() < packedLength) return runs.toByteArray();
        }
        var packed = new byte[packedLength];
        packed[0] = PACKED_GROUPS;
        int ordinal = 0;
        for (int cell = first; cell >= 0; cell = cells.nextSetBit(cell + 1)) {
            setPackedGroup(packed, ordinal++, groupAtCell.applyAsInt(cell));
        }
        return packed;
    }

    private static void appendRun(ByteArrayList output, int group, int length) {
        int value = (length - 1) << GROUP_BITS | group;
        while (value >= 128) {
            output.add((byte) (value | 128));
            value >>>= 7;
        }
        output.add((byte) value);
    }

    private static int groupBytes(int cellCount) {
        return (cellCount * GROUP_BITS + Byte.SIZE - 1) / Byte.SIZE;
    }

    private static byte[] unpackGroups(byte[] encoded, int count) {
        byte[] result = new byte[count];
        if (count == 0) return result;
        int mode = Byte.toUnsignedInt(encoded[0]);
        if (mode < MAX_LOGICAL_CHUNKS && encoded.length == 1) Arrays.fill(result, (byte) mode);
        else if (mode == PACKED_GROUPS && encoded.length == 1 + groupBytes(count)) {
            for (int i = 0; i < count; i++) result[i] = (byte) packedGroup(encoded, i);
        } else if (mode == GROUP_RUNS) {
            int offset = 1, next = 0;
            while (offset < encoded.length && next < count) {
                int value = 0, shift = 0, part;
                do {
                    if (offset == encoded.length || shift == 21)
                        throw new IllegalArgumentException("Invalid construction group run");
                    part = Byte.toUnsignedInt(encoded[offset++]);
                    value |= (part & 127) << shift;
                    shift += 7;
                } while ((part & 128) != 0);
                int end = next + (value >>> GROUP_BITS) + 1;
                if (end > count) throw new IllegalArgumentException("Construction group run exceeds cell count");
                Arrays.fill(result, next, end, (byte) (value & (MAX_LOGICAL_CHUNKS - 1)));
                next = end;
            }
            if (next != count || offset != encoded.length)
                throw new IllegalArgumentException("Invalid construction group coverage");
        } else throw new IllegalArgumentException("Invalid construction group encoding");
        return result;
    }

    static void write(ByteBuf buffer, ConstructionEffectPayload payload) {
        int start = buffer.writerIndex();
        int kind = switch (payload) {
            case Geometry geometry -> geometry.snapshot ? (geometry.layout != null ? 0 : 2) : 1;
            case Visibility visibility -> visibility.visible ? 4 : 3;
            case End _ -> 5;
            case Hud hud -> hud.layout == null ? 6 : 7;
        };
        buffer.writeByte(kind).writeInt(payload.dimension()).writeLong(payload.taskId());
        switch (payload) {
            case Geometry geometry -> {
                buffer.writeLong(geometry.serverTick);
                writeSection(buffer, geometry.section, geometry.quadrant);
                var layout = geometry.layout;
                if (geometry.snapshot) {
                    if (layout != null) writeLayout(buffer, layout);
                    writeCells(buffer, geometry.cells);
                    writeCells(buffer, geometry.fluids);
                    if (!geometry.cells.isEmpty())
                        buffer.writeShort(geometry.groups.length).writeBytes(geometry.groups);
                } else writeCells(buffer, geometry.excluded);
                buffer.writeByte(geometry.regions.size());
                for (var time : geometry.regions) buffer.writeByte(time.region()).writeLong(time.settledTick());
            }
            case Visibility _ -> {
            }
            case End end -> buffer.writeLong(end.serverTick).writeBoolean(end.cancelled).writeBoolean(end.successful);
            case Hud hud -> {
                if (hud.layout != null) writeLayout(buffer, hud.layout);
                buffer.writeInt(hud.status.missing).writeInt(hud.status.errors).writeInt(hud.status.unloaded);
            }
        }
        if (buffer.writerIndex() - start > MAX_BYTES)
            throw new IllegalArgumentException("Construction geometry exceeds packet limit");
    }

    static ConstructionEffectPayload read(ByteBuf buffer) {
        int bytes = buffer.readableBytes();
        if (bytes < HEADER_BYTES || bytes > MAX_BYTES)
            throw new IllegalArgumentException("Invalid construction message length");
        int kind = buffer.readUnsignedByte();
        int dimension = buffer.readInt();
        long task = buffer.readLong();
        ConstructionEffectPayload result = switch (kind) {
            case 0, 1, 2 -> {
                long serverTick = buffer.readLong();
                var section = readSection(buffer);
                int quadrant = buffer.readUnsignedByte();
                boolean snapshot = kind != 1;
                var layout = kind == 0 ? readLayout(buffer) : null;
                BitSet cells = snapshot ? readCells(buffer) : new BitSet();
                BitSet fluids = snapshot ? readCells(buffer) : new BitSet();
                byte[] groups = NO_GROUPS;
                if (!cells.isEmpty()) {
                    int size = buffer.readUnsignedShort();
                    if (size == 0 || size > 1 + groupBytes(cells.cardinality()) || size > buffer.readableBytes())
                        throw new IllegalArgumentException("Invalid construction group encoding length");
                    groups = new byte[size];
                    buffer.readBytes(groups);
                }
                var excluded = snapshot ? new BitSet() : readCells(buffer);
                int count = buffer.readUnsignedByte();
                if (count > REGION_COUNT) throw new IllegalArgumentException("Invalid construction region count");
                List<RegionTime> regions;
                if (count == 0) regions = List.of();
                else {
                    var times = new ObjectArrayList<RegionTime>(count);
                    for (int i = 0; i < count; i++)
                        times.add(new RegionTime(buffer.readUnsignedByte(), buffer.readLong()));
                    regions = times;
                }
                yield new Geometry(dimension, task, serverTick, section, quadrant, snapshot, layout, cells, fluids, groups, excluded, regions);
            }
            case 3, 4 -> new Visibility(dimension, task, kind == 4);
            case 5 -> new End(dimension, task, buffer.readLong(), readBoolean(buffer), readBoolean(buffer));
            case 6, 7 -> new Hud(dimension, task, kind == 7 ? readLayout(buffer) : null,
                new Status(buffer.readInt(), buffer.readInt(), buffer.readInt()));
            default -> throw new IllegalArgumentException("Unknown construction message kind");
        };
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing construction message bytes");
        return result;
    }

    static int bytes(ConstructionEffectPayload payload) {
        return HEADER_BYTES + switch (payload) {
            case Geometry geometry -> geometryBytes(geometry);
            case Visibility _ -> 0;
            case End _ -> 10;
            case Hud hud -> 3 * Integer.BYTES + (hud.layout == null ? 0 : LAYOUT_BYTES);
        };
    }

    private static int geometryBytes(Geometry geometry) {
        int bytes = 19 + (geometry.snapshot ? cellBytes(geometry.cells) + cellBytes(geometry.fluids)
            + (geometry.cells.isEmpty() ? 0 : Short.BYTES + geometry.groups.length) : cellBytes(geometry.excluded));
        if (geometry.layout != null) bytes += LAYOUT_BYTES;
        return bytes + geometry.regions.size() * (Byte.BYTES + Long.BYTES);
    }

    private static Layout readLayout(ByteBuf buffer) {
        return new Layout(readBoolean(buffer), buffer.readLong(), buffer.readUnsignedByte(), buffer.readUnsignedByte(),
            new ConstructionArea(BlockPos.fromLong(buffer.readLong()), BlockPos.fromLong(buffer.readLong())));
    }

    private static void writeLayout(ByteBuf buffer, Layout layout) {
        buffer.writeBoolean(layout.dismantle).writeLong(layout.startTick).writeByte(layout.wireTicks).writeByte(layout.chunks);
        buffer.writeLong(layout.area.min().toLong()).writeLong(layout.area.max().toLong());
    }

    private static void requireTask(long taskId) {
        if (taskId <= 0) throw new IllegalArgumentException("Invalid construction task id");
    }

    private static boolean readBoolean(ByteBuf buffer) {
        int value = buffer.readUnsignedByte();
        if (value > 1) throw new IllegalArgumentException("Invalid construction message flag");
        return value != 0;
    }

    private static Section readSection(ByteBuf buffer) {
        return new Section(buffer.readInt(), buffer.readUnsignedByte(), buffer.readInt());
    }

    private static void writeSection(ByteBuf buffer, Section section, int quadrant) {
        buffer.writeInt(section.x).writeByte(section.y).writeInt(section.z).writeByte(quadrant);
    }

    private static int cellBytes(BitSet cells) {
        return 2 + Math.min(cells.cardinality() * Short.BYTES, CHUNK_CELL_COUNT / Byte.SIZE);
    }

    private static void writeCells(ByteBuf buffer, BitSet cells) {
        int count = cells.cardinality();
        int bitmapBytes = CHUNK_CELL_COUNT / Byte.SIZE;
        if (count * Short.BYTES <= bitmapBytes) {
            buffer.writeShort(count);
            for (int index = cells.nextSetBit(0); index >= 0; index = cells.nextSetBit(index + 1))
                buffer.writeShort(index);
        } else {
            buffer.writeShort(0xffff);
            byte[] values = cells.toByteArray();
            buffer.writeBytes(values).writeZero(bitmapBytes - values.length);
        }
    }

    private static BitSet readCells(ByteBuf buffer) {
        int count = buffer.readUnsignedShort();
        int bitmapBytes = CHUNK_CELL_COUNT / Byte.SIZE;
        if (count == 0xffff) {
            byte[] values = new byte[bitmapBytes];
            buffer.readBytes(values);
            var cells = BitSet.valueOf(values);
            if (cells.cardinality() * Short.BYTES <= bitmapBytes)
                throw new IllegalArgumentException("Noncanonical construction geometry bitmap");
            return cells;
        }
        if (count * Short.BYTES > bitmapBytes || buffer.readableBytes() < count * Short.BYTES)
            throw new IllegalArgumentException("Invalid construction geometry cell count");
        var cells = new BitSet();
        for (int i = 0, previous = -1; i < count; i++) {
            int index = buffer.readUnsignedShort();
            if (index <= previous || index >= CHUNK_CELL_COUNT)
                throw new IllegalArgumentException("Invalid construction geometry cell index");
            previous = index;
            cells.set(index);
        }
        return cells;
    }

    int dimension();

    long taskId();

    record RegionTime(int region, long settledTick) {
        public RegionTime {
            if (region < 0 || region >= REGION_COUNT || settledTick < 0)
                throw new IllegalArgumentException("Invalid construction region time");
        }
    }

    record End(int dimension, long taskId, long serverTick, boolean cancelled,
               boolean successful) implements ConstructionEffectPayload {
        public End {
            requireTask(taskId);
            if (serverTick < 0 || cancelled && successful)
                throw new IllegalArgumentException("Invalid construction end status");
        }
    }

    record Section(int x, int y, int z) {
        public Section {
            if (x < -937500 || x > 937499 || z < -937500 || z > 937499 || y < 0 || y > 7)
                throw new IllegalArgumentException("Invalid construction section");
        }

        public static Section at(BlockPos pos) {
            return new Section(pos.getX() >> 5, pos.getY() >> 5, pos.getZ() >> 5);
        }

        public static int quadrant(BlockPos pos) {
            return ((pos.getX() >> 4) & 1) | (((pos.getZ() >> 4) & 1) << 1);
        }

        public static int index(BlockPos pos) {
            return (pos.getX() & 15) | ((pos.getZ() & 15) << 4) | ((pos.getY() & 31) << 8);
        }

        public int chunkX(int quadrant) {
            return x * 2 + (quadrant & 1);
        }

        public int chunkZ(int quadrant) {
            return z * 2 + (quadrant >>> 1);
        }
    }

    record Layout(boolean dismantle, long startTick, int wireTicks, int chunks, ConstructionArea area) {
        public Layout {
            Objects.requireNonNull(area);
            if (startTick < 0 || wireTicks < MIN_INITIAL_WIRE_TICKS || wireTicks > MAX_INITIAL_WIRE_TICKS
                || chunks <= 0 || chunks > MAX_LOGICAL_CHUNKS)
                throw new IllegalArgumentException("Invalid construction layout");
        }
    }

    record Geometry(int dimension, long taskId, long serverTick, Section section, int quadrant, boolean snapshot,
                    @Nullable Layout layout, BitSet cells, BitSet fluids, byte[] groups,
                    BitSet excluded, List<RegionTime> regions) implements ConstructionEffectPayload {
        public Geometry {
            requireTask(taskId);
            if (serverTick < 0 || quadrant < 0 || quadrant > 3
                || cells.length() > CHUNK_CELL_COUNT || fluids.length() > CHUNK_CELL_COUNT || excluded.length() > CHUNK_CELL_COUNT)
                throw new IllegalArgumentException("Invalid construction geometry bounds");
            if (snapshot) {
                for (int cell = fluids.nextSetBit(0); cell >= 0; cell = fluids.nextSetBit(cell + 1)) {
                    if (!cells.get(cell)) throw new IllegalArgumentException("Construction fluid without a cell");
                }
                if (layout != null && (serverTick < layout.startTick || cells.isEmpty()) || cells.isEmpty() != (groups.length == 0)
                    || groups.length > 1 + groupBytes(cells.cardinality())
                    || !excluded.isEmpty())
                    throw new IllegalArgumentException("Invalid construction geometry snapshot");
            } else if (layout != null || groups.length != 0 || !cells.isEmpty() || !fluids.isEmpty()
                || regions.isEmpty() && excluded.isEmpty())
                throw new IllegalArgumentException("Invalid construction geometry delta");
            long seen = 0;
            for (var time : regions) {
                long mask = 1L << time.region();
                int group = time.region() >= MAX_LOGICAL_CHUNKS ? time.region() - MAX_LOGICAL_CHUNKS : time.region();
                if ((seen & mask) != 0 || time.settledTick() > serverTick
                    || layout != null && group >= layout.chunks()
                    || layout != null && time.settledTick() < layout.startTick)
                    throw new IllegalArgumentException("Conflicting construction region time");
                seen |= mask;
            }
            // Builders and decoders transfer ownership. Packet contents remain read only after this point.
        }

        public static int sectionIndex(int quadrant, int index) {
            int x = (index & 15) | ((quadrant & 1) << 4);
            int y = index >>> 8;
            int z = ((index >>> 4) & 15) | ((quadrant & 2) << 3);
            return x | (z << 5) | (y << 10);
        }

        public byte[] cellGroups() {
            return unpackGroups(groups, cells.cardinality());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Geometry(
                var otherDimension, var otherTaskId, var otherTick, var otherSection, var otherQuadrant,
                var otherSnapshot, var otherLayout, var otherCells, var otherFluids, var otherGroups, var otherExcluded,
                var otherTimes
            ))) return false;
            return dimension == otherDimension && taskId == otherTaskId && serverTick == otherTick && quadrant == otherQuadrant
                && snapshot == otherSnapshot && section.equals(otherSection) && Objects.equals(layout, otherLayout) && cells.equals(otherCells)
                && fluids.equals(otherFluids) && Arrays.equals(groups, otherGroups) && excluded.equals(otherExcluded) && regions.equals(otherTimes);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(dimension, taskId, serverTick, section, quadrant, snapshot, layout, cells, fluids, excluded, regions);
            return 31 * result + Arrays.hashCode(groups);
        }

        public int sectionIndex(int index) {
            return sectionIndex(quadrant, index);
        }
    }

    record Status(int missing, int errors, int unloaded) {
        public Status {
            if (missing < 0 || errors < 0 || unloaded < 0)
                throw new IllegalArgumentException("Invalid construction status");
        }
    }

    record Hud(int dimension, long taskId, @Nullable Layout layout, Status status) implements ConstructionEffectPayload {
        public Hud {
            requireTask(taskId);
            Objects.requireNonNull(status);
        }
    }

    record Visibility(int dimension, long taskId, boolean visible) implements ConstructionEffectPayload {
        public Visibility {
            requireTask(taskId);
        }
    }
}
