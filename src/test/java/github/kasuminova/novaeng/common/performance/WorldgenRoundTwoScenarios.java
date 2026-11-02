package github.kasuminova.novaeng.common.performance;

import appeng.core.worlddata.MeteorDataNameEncoder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.kasuminova.novaeng.common.performance.SpawnPerformanceBenchmark.Scenario;
import github.kasuminova.novaeng.common.performance.SpawnPerformanceBenchmark.Workload;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.BlockStone;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

final class WorldgenRoundTwoScenarios {

    private WorldgenRoundTwoScenarios() {
    }

    static List<Scenario> create() {
        try {
            final List<Scenario> cases = new ObjectArrayList<>(WorldgenHotspotScenarios.create().subList(0, 2));
            cases.add(filename());
            cases.add(fileRead(256));
            cases.add(fileRead(16384));
            cases.add(stoneStates());
            cases.add(placement());
            cases.add(vein(8));
            cases.add(vein(24));
            cases.add(vein(42));
            return cases;
        } catch (final Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static Scenario filename() {
        final MeteorDataNameEncoder encoder = new MeteorDataNameEncoder(4);
        final Workload before = new Workload() {
            private int coordinate;

            public long step() {
                return encoder.encode(-300, this.coordinate++, -60000).hashCode();
            }
        };
        final Workload after = new Workload() {
            private int coordinate;

            public long step() {
                return MeteorDataFilename.encode(-300, this.coordinate++, -60000, 4, '_', '.', "dat").hashCode();
            }
        };
        return checked("meteor_region_filename", before, after);
    }

    private static Scenario fileRead(final int payloadSize) throws Exception {
        final Path file = Files.createTempFile("nova-meteor-benchmark-", ".dat");
        file.toFile().deleteOnExit();
        final NBTTagCompound data = new NBTTagCompound();
        final byte[] payload = new byte[payloadSize];
        new Random(42).nextBytes(payload);
        data.setByteArray("payload", payload);
        data.setInteger("num", 7);
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(data, bytes);
        Files.write(file, bytes.toByteArray());
        final Workload before = () -> read(file, false);
        final Workload after = () -> read(file, true);
        return checked("meteor_gzip_file_" + payloadSize, before, after);
    }

    private static long read(final Path file, final boolean optimized) {
        try (final FileInputStream source = new FileInputStream(file.toFile())) {
            final NBTTagCompound data = optimized ? MeteorSpawnDataInput.read(source) : CompressedStreamTools.readCompressed(source);
            return data.getInteger("num") + data.getByteArray("payload").length;
        } catch (final Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Scenario stoneStates() throws Exception {
        final BlockStone stone = WorldgenTestBlocks.stone();
        final MethodHandle original = MethodHandles.dropArguments(MethodHandles.lookup()
                                                                               .unreflect(IBlockState.class.getMethod("withProperty", IProperty.class, Comparable.class)), 3, Operation.class);
        final Class<?> wrapper = BlockMetadataStatesTest.wrapperClass();
        final Object instance = wrapper.getConstructor().newInstance();
        final MethodHandle optimized = MethodHandles.lookup().unreflect(BlockMetadataStatesTest.select(wrapper)).bindTo(instance);
        final Operation<IBlockState> operation = args -> ((IBlockState) args[0]).withProperty((IProperty) args[1], (Comparable) args[2]);
        return checked("stone_metadata_transition", metadataWork(stone, original, operation),
            metadataWork(stone, optimized, operation));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Workload metadataWork(final BlockStone stone, final MethodHandle call, final Operation<IBlockState> operation) {
        return new Workload() {
            private int index;

            public long step() {
                try {
                    final IBlockState base = stone.getDefaultState();
                    final BlockStone.EnumType variant = BlockStone.EnumType.byMetadata(this.index++ % 7);
                    final IBlockState result = (IBlockState) call.invokeExact(base, (IProperty) BlockStone.VARIANT,
                        (Comparable) variant, (Operation) operation);
                    return result.getValue(BlockStone.VARIANT).getMetadata();
                } catch (final Throwable failure) {
                    throw new IllegalStateException(failure);
                }
            }
        };
    }

    private static Scenario placement() throws Exception {
        return checked("cofh_material_check_and_placement", placementWork(false), placementWork(true));
    }

    private static Workload placementWork(final boolean optimized) throws Exception {
        final WorldGenSinglePositionTest.Placement placement = WorldGenSinglePositionTest.placement(optimized);
        final MethodHandle call = MethodHandles.lookup().unreflect(placement.method());
        final WorldGenSinglePositionTest.RecordingWorld world = WorldGenSinglePositionTest.world();
        world.recording = false;
        final Random random = new Random(42);
        final Object materials = placement.materials();
        final MethodHandle invoke = call.asType(call.type().changeParameterType(5, Object.class));
        return () -> {
            try {
                final boolean result = (boolean) invoke.invokeExact((World) world, random, 8, 32, -8, materials, placement.ores());
                return world.checksum + (result ? 1 : 0);
            } catch (final Throwable failure) {
                throw new IllegalStateException(failure);
            }
        };
    }

    private static Scenario vein(final int size) throws Exception {
        return checked("cofh_vein_geometry_" + size, veinWork(false, size), veinWork(true, size));
    }

    private static Workload veinWork(final boolean optimized, final int size) throws Exception {
        final OreVeinGeometryTest.Generator generator = OreVeinGeometryTest.factory(optimized).create(size);
        final MethodHandle call = MethodHandles.lookup().unreflect(generator.generate()).bindTo(generator.instance());
        final WorldGenSinglePositionTest.RecordingWorld world = WorldGenSinglePositionTest.world();
        world.recording = false;
        final Random random = new Random(42);
        final BlockPos position = new BlockPos(1000, 32, -1000);
        return () -> {
            try {
                final boolean result = (boolean) call.invokeExact((World) world, random, position);
                return world.checksum + (result ? 1 : 0);
            } catch (final Throwable failure) {
                throw new IllegalStateException(failure);
            }
        };
    }

    private static Scenario checked(final String name, final Workload before, final Workload after) {
        return new Scenario(name, before, after, () -> {
            if (before.step() != after.step()) {
                throw new AssertionError("Different output: " + name);
            }
        });
    }
}
