package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.common.performance.SpawnPerformanceBenchmark.Scenario;
import github.kasuminova.novaeng.common.performance.SpawnPerformanceBenchmark.Workload;
import net.minecraft.block.Block;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.util.math.BlockPos;
import zmaster587.advancedRocketry.api.AreaBlob;
import zmaster587.advancedRocketry.api.IAtmosphere;
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;
import zmaster587.advancedRocketry.api.util.IBlobHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

final class WorldgenHotspotScenarios {

    private static volatile Object escapingResult;

    private WorldgenHotspotScenarios() {
    }

    static List<Scenario> create() {
        try {
            return List.of(temperature(false), temperature(true), classification("AtmosphereTypes", "getAtmosphereTypeFromValue"),
                classification("Temps", "getTempFromValue"), orePredicate(), emptyAtmosphere(), radius(0), radius(8),
                privateMap(false, false), privateMap(false, true), privateMap(true, false), privateMap(true, true));
        } catch (final Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Scenario temperature(final boolean changing) throws ReflectiveOperationException {
        final MethodHandle original = MethodHandles.publicLookup().unreflect(
            AtmosphereTemperatureCacheTest.temperatureMethod(new WorldgenFixtureLoader(false)));
        final MethodHandle optimized = MethodHandles.publicLookup().unreflect(
            AtmosphereTemperatureCacheTest.temperatureMethod(new WorldgenFixtureLoader(true)));
        return checked(changing ? "atmosphere_temperature_changing" : "atmosphere_temperature_repeated",
            temperatureWork(original, changing), temperatureWork(optimized, changing));
    }

    private static Workload temperatureWork(final MethodHandle calculation, final boolean changing) {
        final TemperatureStar star = new TemperatureStar();
        star.setSize(1.0F);
        return new Workload() {
            private int counter;

            @Override
            public long step() {
                star.temperature = 100 + ((this.counter++ / (changing ? 1 : 1024)) & 15);
                try {
                    return (int) calculation.invokeExact((StellarBody) star, 100, 100);
                } catch (final Throwable failure) {
                    throw new IllegalStateException(failure);
                }
            }
        };
    }

    private static Scenario classification(final String type, final String method) throws ReflectiveOperationException {
        final Class<?> before = new WorldgenFixtureLoader(false)
            .loadClass("zmaster587.advancedRocketry.dimension.DimensionProperties$" + type);
        final Class<?> after = new WorldgenFixtureLoader(true).loadClass(before.getName());
        final MethodHandle oldCall = MethodHandles.publicLookup().unreflect(before.getMethod(method, int.class))
                                                  .asType(MethodType.methodType(Object.class, int.class));
        final MethodHandle newCall = MethodHandles.publicLookup().unreflect(after.getMethod(method, int.class))
                                                  .asType(MethodType.methodType(Object.class, int.class));
        return checked("atmosphere_classification_" + type, enumWork(oldCall), enumWork(newCall));
    }

    private static Workload enumWork(final MethodHandle call) {
        return new Workload() {
            private int value;

            @Override
            public long step() {
                try {
                    return ((Enum<?>) (Object) call.invokeExact(this.value++ & 1023)).ordinal();
                } catch (final Throwable failure) {
                    throw new IllegalStateException(failure);
                }
            }
        };
    }

    private static Scenario orePredicate() throws ReflectiveOperationException {
        final Class<?> type = new WorldgenFixtureLoader(true).loadClass("cofh.cofhworld.util.random.WeightedBlock");
        final WeightedBlockPredicate material = (WeightedBlockPredicate) type.getConstructor(Block.class).newInstance((Object) null);
        return checked("cofh_escaping_material_predicate", () -> {
            final BlockMatcher matcher = BlockMatcher.forBlock(null);
            escapingResult = matcher;
            return matcher.apply(null) ? 1 : 0;
        }, () -> {
            final BlockMatcher matcher = material.nova$replacementPredicate();
            escapingResult = matcher;
            return matcher.apply(null) ? 1 : 0;
        });
    }

    private static Scenario emptyAtmosphere() throws Exception {
        final Class<?> before = AtmosphereHandlerFixture.create(false);
        final Class<?> after = AtmosphereHandlerFixture.create(true);
        final MethodHandle oldCall = MethodHandles.publicLookup().unreflect(before.getMethod("getAtmosphereType", BlockPos.class))
                                                  .bindTo(before.getConstructor().newInstance());
        final MethodHandle newCall = MethodHandles.publicLookup().unreflect(after.getMethod("getAtmosphereType", BlockPos.class))
                                                  .bindTo(after.getConstructor().newInstance());
        final BlockPos position = new BlockPos(12, 64, -12);
        return checked("atmosphere_empty_blob_lookup", atmosphereWork(oldCall, position), atmosphereWork(newCall, position));
    }

    private static Workload atmosphereWork(final MethodHandle lookup, final BlockPos position) {
        return () -> {
            try {
                final IAtmosphere result = (IAtmosphere) lookup.invokeExact(position);
                return result == null ? 0 : 1;
            } catch (final Throwable failure) {
                throw new IllegalStateException(failure);
            }
        };
    }

    private static Scenario radius(final int count) throws Exception {
        return checked("atmosphere_radius_" + count, radiusWork(false, count), radiusWork(true, count));
    }

    @SuppressWarnings("unchecked")
    private static Workload radiusWork(final boolean optimized, final int count) throws Exception {
        final Class<?> type = AtmosphereHandlerFixture.create(optimized);
        final Object handler = type.getConstructor().newInstance();
        final Map<IBlobHandler, AreaBlob> blobs = (Map<IBlobHandler, AreaBlob>) type.getField("blobs").get(handler);
        for (int i = 0; i < count; i++) {
            final int id = i;
            final HashedBlockPosition root = new HashedBlockPosition(i, 0, 0);
            final AreaBlob blob = new AreaBlob(null) {
                @Override
                public HashedBlockPosition getRootPosition() {
                    return root;
                }
            };
            final IBlobHandler key = (IBlobHandler) Proxy.newProxyInstance(WorldgenHotspotScenarios.class.getClassLoader(),
                new Class<?>[]{IBlobHandler.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "hashCode" -> id;
                    case "equals" -> proxy == args[0];
                    default -> null;
                });
            blobs.put(key, blob);
        }
        final HashedBlockPosition position = new HashedBlockPosition(0, 0, 0);
        final MethodHandle select = MethodHandles.publicLookup().unreflect(type.getMethod("getBlobWithinRadius",
            HashedBlockPosition.class, int.class)).bindTo(handler);
        return () -> {
            try {
                final List<?> result = (List<?>) select.invokeExact(position, 256);
                escapingResult = result;
                return result.size();
            } catch (final Throwable failure) {
                throw new IllegalStateException(failure);
            }
        };
    }

    private static Scenario privateMap(final boolean longs, final boolean replace) throws Exception {
        final Class<?> fixture = longs ? PrivateIdMapsMixinTest.LongFixture.class : PrivateIdMapsMixinTest.IntFixture.class;
        final Class<?> specialized = longs ? PrivateIdMapsMixinTest.PrimitiveLongFixture.class : PrivateIdMapsMixinTest.PrimitiveIntFixture.class;
        return checked("private_" + (longs ? "long" : "int") + "_map_" + (replace ? "replace" : "lookup"),
            mapWork(fixture, longs, replace), mapWork(specialized, longs, replace));
    }

    private static Workload mapWork(final Class<?> type, final boolean longs, final boolean replace) throws Exception {
        final Class<?> key = longs ? long.class : int.class;
        MethodHandle put = MethodHandles.publicLookup().unreflect(type.getMethod("put", key, Object.class));
        MethodHandle get = MethodHandles.publicLookup().unreflect(type.getMethod("get", key));
        if (longs) {
            final Object instance = type.getConstructor().newInstance();
            put = put.bindTo(instance);
            get = get.bindTo(instance);
        }
        final Integer value = 42;
        try {
            for (int i = 0; i < 64; i++) {
                if (longs) {
                    final Object ignored = (Object) put.invokeExact(0x100000000L + i, (Object) value);
                } else {
                    final Object ignored = (Object) put.invokeExact(1000 + i, (Object) value);
                }
            }
        } catch (final Throwable failure) {
            throw new IllegalStateException(failure);
        }
        final MethodHandle operation = replace ? put : get;
        return new Workload() {
            private int index;

            @Override
            public long step() {
                try {
                    final int offset = this.index++ & 63;
                    final Object result;
                    if (longs) {
                        result = replace ? (Object) operation.invokeExact(0x100000000L + offset, (Object) value)
                            : (Object) operation.invokeExact(0x100000000L + offset);
                    } else {
                        result = replace ? (Object) operation.invokeExact(1000 + offset, (Object) value)
                            : (Object) operation.invokeExact(1000 + offset);
                    }
                    return ((Integer) result).longValue();
                } catch (final Throwable failure) {
                    throw new IllegalStateException(failure);
                }
            }
        };
    }

    private static Scenario checked(final String name, final Workload original, final Workload optimized) {
        return new Scenario(name, original, optimized, () -> {
            if (original.step() != optimized.step()) {
                throw new AssertionError("Different result: " + name);
            }
        });
    }

    private static final class TemperatureStar extends StellarBody {
        private int temperature;

        @Override
        public int getTemperature() {
            return this.temperature;
        }
    }
}
