package github.kasuminova.novaeng.common.performance;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import zmaster587.advancedRocketry.api.AreaBlob;
import zmaster587.advancedRocketry.api.IAtmosphere;
import zmaster587.advancedRocketry.api.util.IBlobHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AtmosphereHandlerTest {

    static IAtmosphere atmosphere() {
        return (IAtmosphere) Proxy.newProxyInstance(AtmosphereHandlerTest.class.getClassLoader(),
            new Class<?>[]{IAtmosphere.class}, (proxy, method, args) -> null);
    }

    @SuppressWarnings("unchecked")
    private static Map<IBlobHandler, AreaBlob> blobs(final Class<?> type, final Object handler) throws Exception {
        return (Map<IBlobHandler, AreaBlob>) type.getField("blobs").get(handler);
    }

    @Test
    void emptyAndPopulatedRegionsKeepLiveDefaultAndBlobDecisions() throws Exception {
        final Class<?> beforeType = AtmosphereHandlerFixture.create(false);
        final Class<?> afterType = AtmosphereHandlerFixture.create(true);
        final Object before = beforeType.getConstructor().newInstance();
        final Object after = afterType.getConstructor().newInstance();
        final Method oldGet = beforeType.getMethod("getAtmosphereType", BlockPos.class);
        final Method newGet = afterType.getMethod("getAtmosphereType", BlockPos.class);
        final BlockPos position = new BlockPos(12, 64, -12);
        final IAtmosphere first = atmosphere();
        final IAtmosphere second = atmosphere();
        for (final IAtmosphere current : new IAtmosphere[]{first, second}) {
            beforeType.getField("atmosphere").set(before, current);
            afterType.getField("atmosphere").set(after, current);
            assertSame(oldGet.invoke(before, position), newGet.invoke(after, position));
        }
        assertEquals(2, afterType.getField("defaultCalls").getInt(after));
        final AreaBlob blob = new AreaBlob(null) {
            @Override
            public boolean contains(final HashedBlockPosition candidate) {
                return candidate.x == 12;
            }
        };
        final Map<IBlobHandler, AreaBlob> oldBlobs = blobs(beforeType, before);
        final Map<IBlobHandler, AreaBlob> newBlobs = blobs(afterType, after);
        oldBlobs.put(null, blob);
        newBlobs.put(null, blob);
        blob.setData(null);
        assertSame(oldGet.invoke(before, position), newGet.invoke(after, position));
        blob.setData(first);
        assertSame(first, newGet.invoke(after, position));
        assertSame(oldGet.invoke(before, position), newGet.invoke(after, position));
        oldBlobs.clear();
        newBlobs.clear();
        assertSame(second, newGet.invoke(after, position));
        beforeType.getField("oxygen").setBoolean(null, false);
        afterType.getField("oxygen").setBoolean(null, false);
        assertSame(oldGet.invoke(before, position), newGet.invoke(after, position));
    }

    @Test
    void radiusResultsKeepOrderBoundsAndIndependentMutability() throws Exception {
        final Class<?> beforeType = AtmosphereHandlerFixture.create(false);
        final Class<?> afterType = AtmosphereHandlerFixture.create(true);
        final Object before = beforeType.getConstructor().newInstance();
        final Object after = afterType.getConstructor().newInstance();
        for (int i = 0; i < 8; i++) {
            final int coordinate = i;
            final AreaBlob blob = new AreaBlob(null) {
                @Override
                public HashedBlockPosition getRootPosition() {
                    return new HashedBlockPosition(coordinate, 0, 0);
                }
            };
            final IBlobHandler key = (IBlobHandler) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IBlobHandler.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "hashCode" -> coordinate;
                    case "equals" -> proxy == args[0];
                    default -> null;
                });
            blobs(beforeType, before).put(key, blob);
            blobs(afterType, after).put(key, blob);
        }
        final Method oldRadius = beforeType.getMethod("getBlobWithinRadius", HashedBlockPosition.class, int.class);
        final Method newRadius = afterType.getMethod("getBlobWithinRadius", HashedBlockPosition.class, int.class);
        for (int radius = -1; radius <= 9; radius++) {
            final HashedBlockPosition position = new HashedBlockPosition(0, 0, 0);
            final List<?> expected = (List<?>) oldRadius.invoke(before, position, radius);
            final List<?> actual = (List<?>) newRadius.invoke(after, position, radius);
            assertEquals(expected, actual);
            actual.clear();
            assertEquals(expected, newRadius.invoke(after, position, radius));
        }
    }
}
