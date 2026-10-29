package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.common.performance.SpawnPerformanceBenchmark.Scenario;
import github.kasuminova.novaeng.common.performance.SpawnPerformanceBenchmark.Workload;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

final class WorldgenRoundThreeScenarios {

    private static volatile Object escapingSnapshot;

    private WorldgenRoundThreeScenarios() {
    }

    static List<Scenario> create() {
        try {
            final List<Scenario> cases = new ObjectArrayList<>();
            cases.add(compass("compass_missing_one_target", -1, 1));
            cases.add(compass("compass_missing_sixteen_targets", -1, 16));
            cases.add(compass("compass_late_target", 8191, 1));
            cases.add(compass("compass_first_target", 0, 1));
            for (final int size : new int[]{8, 24, 64}) {
                cases.add(checked("vanilla_ore_geometry_" + size, vein(false, size), vein(true, size)));
            }
            cases.add(checked("biome_zoom_16", zoom(false, false, 16, false), zoom(true, false, 16, false)));
            cases.add(checked("biome_fuzzy_zoom_16", zoom(false, true, 16, false), zoom(true, true, 16, false)));
            cases.add(checked("biome_zoom_64", zoom(false, false, 64, false), zoom(true, false, 64, false)));
            cases.add(checked("biome_zoom_chain_16", zoom(false, false, 16, true), zoom(true, false, 16, true)));
            for (final int count : new int[]{0, 3, 8, 32}) {
                cases.add(checked("attribute_modifier_snapshot_" + count, modifiers(false, count), modifiers(true, count)));
            }
            return cases;
        } catch (final Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static Scenario compass(final String name, final int match, final int targets) throws Exception {
        return checked(name, compassWork(false, match, targets), compassWork(true, match, targets));
    }

    private static Workload compassWork(final boolean optimized, final int match, final int targetCount) throws Exception {
        final MeteorCompassMetadataTest.Scan scan = MeteorCompassMetadataTest.scan(optimized, match, false);
        final BlockStone stone = MeteorCompassMetadataTest.registered(WorldgenTestBlocks.stone(), new ResourceLocation("nova", "stone"));
        final IBlockState[] states = scan.world().chunk.states;
        Arrays.fill(states, stone.getDefaultState());
        if (match >= 0) {
            states[match] = scan.target().getDefaultState().withProperty(MeteorCompassMetadataTest.META, 3);
        }
        final List<ResourceLocation> names = new ObjectArrayList<>();
        final List<Integer> metadata = new ObjectArrayList<>();
        for (int i = 1; i < targetCount; i++) {
            names.add(new ResourceLocation("nova", "absent_" + i));
            metadata.add(3);
        }
        names.add(scan.target().getRegistryName());
        metadata.add(3);
        scan.targets(names, metadata);
        final MethodHandle call = MethodHandles.lookup().unreflect(scan.method()).bindTo(scan.instance());
        return () -> {
            try {
                final CallbackInfoReturnable<Future<?>> callback = new CallbackInfoReturnable<>("benchmark", true);
                call.invokeExact((World) scan.world(), 0, 32, 0, (CallbackInfoReturnable) callback);
                return scan.world().chunk.checksum + (scan.executor().last.found ? 1 : 0);
            } catch (final Throwable failure) {
                throw new IllegalStateException(failure);
            }
        };
    }

    private static Workload vein(final boolean optimized, final int size) throws Exception {
        final VanillaOreGeometryTest.Generator generator = VanillaOreGeometryTest.factory(optimized).create(size, state -> true);
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

    private static Workload zoom(final boolean optimized, final boolean fuzzy, final int size, final boolean chain) throws Exception {
        final GenLayerZoomRandomTest.PatternLayer pattern = new GenLayerZoomRandomTest.PatternLayer(2);
        pattern.recording = false;
        final GenLayer layer = chain ? GenLayerZoomRandomTest.chain(optimized) : GenLayerZoomRandomTest.layer(optimized, fuzzy, pattern);
        layer.initWorldGenSeed(42);
        final MethodHandle call = MethodHandles.lookup().findVirtual(layer.getClass(), "getInts",
            MethodType.methodType(int[].class, int.class, int.class, int.class, int.class)).bindTo(layer);
        return new Workload() {
            private int coordinate;

            public long step() {
                try {
                    IntCache.resetIntCache();
                    final int[] values = (int[]) call.invokeExact(this.coordinate++, -1000, size, size);
                    long checksum = 0;
                    for (int i = 0; i < size * size; i++) {
                        checksum = checksum * 31 + values[i];
                    }
                    return checksum;
                } catch (final Throwable failure) {
                    throw new IllegalStateException(failure);
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Workload modifiers(final boolean optimized, final int count) throws Exception {
        final IAttributeInstance instance = AttributeModifierSnapshotTest.create(optimized, count);
        final MethodHandle call = MethodHandles.lookup().findVirtual(instance.getClass(), "getModifiers",
            MethodType.methodType(Collection.class)).bindTo(instance);
        return () -> {
            try {
                final Collection<AttributeModifier> result = (Collection<AttributeModifier>) call.invokeExact();
                escapingSnapshot = result;
                long checksum = result.size();
                for (final AttributeModifier modifier : result) {
                    checksum += modifier.getID().getLeastSignificantBits();
                }
                return checksum;
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
