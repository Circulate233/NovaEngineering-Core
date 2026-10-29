package github.kasuminova.novaeng.common.util;

import crafttweaker.api.data.DataMap;
import crafttweaker.api.data.IData;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import stanhebben.zenscript.annotations.ZenMethod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public final class IDataUtils {

    private IDataUtils() {
    }

    private static Map<String, IData> getNBTMap(final IData data) {
        return data instanceof DataMap map ? map.asMap() : Object2ObjectMaps.emptyMap();
    }

    @ZenMethod
    @Nullable
    public static IData get(final IData data, final String path, @Nullable final IData defaultValue) {
        final Map<String, IData> values = getNBTMap(data);
        return values.getOrDefault(path, defaultValue);
    }

    @ZenMethod
    public static boolean getBool(final IData data, final String path, final boolean defaultValue) {
        final Map<String, IData> values = getNBTMap(data);
        if (!values.containsKey(path)) {
            return defaultValue;
        }
        final IData value = values.get(path);
        return value != null && value.asBool();
    }

    @ZenMethod
    public static byte getByte(final IData data, final String path, final byte defaultValue) {
        final Map<String, IData> values = getNBTMap(data);
        if (!values.containsKey(path)) {
            return defaultValue;
        }
        final IData value = values.get(path);
        return value == null ? 0 : value.asByte();
    }

    @ZenMethod
    public static double getDouble(final IData data, final String path, final double defaultValue) {
        final Map<String, IData> values = getNBTMap(data);
        if (!values.containsKey(path)) {
            return defaultValue;
        }
        final IData value = values.get(path);
        return value == null ? 0.0D : value.asDouble();
    }

    @ZenMethod
    public static float getFloat(final IData data, final String path, final float defaultValue) {
        final Map<String, IData> values = getNBTMap(data);
        if (!values.containsKey(path)) {
            return defaultValue;
        }
        final IData value = values.get(path);
        return value == null ? 0.0F : value.asFloat();
    }

    @ZenMethod
    public static int getInt(final IData data, final String path, final int defaultValue) {
        final Map<String, IData> values = getNBTMap(data);
        if (!values.containsKey(path)) {
            return defaultValue;
        }
        final IData value = values.get(path);
        return value == null ? 0 : value.asInt();
    }

    @ZenMethod
    @Nullable
    public static List<IData> getList(final IData data, final String path, @Nullable final List<IData> defaultValue) {
        final Map<String, IData> values = getNBTMap(data);
        if (!values.containsKey(path)) {
            return defaultValue;
        }
        final IData value = values.get(path);
        return value == null ? null : value.asList();
    }

    @ZenMethod
    public static long getLong(final IData data, final String path, final long defaultValue) {
        final Map<String, IData> values = getNBTMap(data);
        if (!values.containsKey(path)) {
            return defaultValue;
        }
        final IData value = values.get(path);
        return value == null ? 0L : value.asLong();
    }

    @ZenMethod
    @Nullable
    public static Map<String, IData> getMap(final IData data, final String path, @Nullable final Map<String, IData> defaultValue) {
        final Map<String, IData> values = getNBTMap(data);
        if (!values.containsKey(path)) {
            return defaultValue;
        }
        final IData value = values.get(path);
        return value == null ? null : value.asMap();
    }

    @ZenMethod
    public static short getShort(final IData data, final String path, final short defaultValue) {
        final Map<String, IData> values = getNBTMap(data);
        if (!values.containsKey(path)) {
            return defaultValue;
        }
        final IData value = values.get(path);
        return value == null ? 0 : value.asShort();
    }

    @ZenMethod
    public static String getString(final IData data, final String path, final String defaultValue) {
        final Map<String, IData> values = getNBTMap(data);
        if (!values.containsKey(path)) {
            return defaultValue;
        }
        final IData value = values.get(path);
        return value == null ? "" : value.asString();
    }

    @ZenMethod
    @Nullable
    public static byte[] getByteArray(final IData data, final String path, @Nullable final byte[] defaultValue) {
        final Map<String, IData> values = getNBTMap(data);
        if (!values.containsKey(path)) {
            return defaultValue;
        }
        final IData value = values.get(path);
        return value == null ? null : value.asByteArray();
    }

    @ZenMethod
    @Nullable
    public static int[] getIntArray(final IData data, final String path, @Nullable final int[] defaultValue) {
        final Map<String, IData> values = getNBTMap(data);
        if (!values.containsKey(path)) {
            return defaultValue;
        }
        final IData value = values.get(path);
        return value == null ? null : value.asIntArray();
    }

    @ZenMethod
    public static boolean check(final IData data, @Nullable final String path) {
        return getNBTMap(data).containsKey(path);
    }

    @ZenMethod
    public static boolean check(final IData data, final String... paths) {
        final Map<String, IData> values = getNBTMap(data);
        for (final String path : paths) {
            if (!values.containsKey(path)) {
                return false;
            }
        }
        return true;
    }
}
