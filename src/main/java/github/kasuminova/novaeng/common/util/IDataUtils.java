package github.kasuminova.novaeng.common.util;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.data.DataMap;
import crafttweaker.api.data.IData;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.List;
import java.util.Map;

@ZenRegister
@ZenExpansion("crafttweaker.data.IData")
public class IDataUtils {

    private static Map<String, IData> get(IData data) {
        if (data instanceof DataMap) {
            return data.asMap();
        }
        return Object2ObjectMaps.emptyMap();
    }

    @ZenMethod
    public static IData get(IData data, String path, @Optional IData defaultValue) {
        if (check(data, path)) {
            return get(data).get(path);
        }
        return defaultValue;
    }

    @ZenMethod
    public static boolean getBool(IData data, String path, @Optional boolean defaultValue) {
        if (check(data, path)) {
            return get(data).get(path).asBool();
        }
        return defaultValue;
    }

    @ZenMethod
    public static byte getByte(IData data, String path, @Optional byte defaultValue) {
        if (check(data, path)) {
            return get(data).get(path).asByte();
        }
        return defaultValue;
    }

    @ZenMethod
    public static double getDouble(IData data, String path, @Optional double defaultValue) {
        if (check(data, path)) {
            return get(data).get(path).asDouble();
        }
        return defaultValue;
    }

    @ZenMethod
    public static float getFloat(IData data, String path, @Optional float defaultValue) {
        if (check(data, path)) {
            return get(data).get(path).asFloat();
        }
        return defaultValue;
    }

    @ZenMethod
    public static int getInt(IData data, String path, @Optional int defaultValue) {
        if (check(data, path)) {
            return get(data).get(path).asInt();
        }
        return defaultValue;
    }

    @ZenMethod
    public static List<IData> getList(IData data, String path, @Optional List<IData> defaultValue) {
        if (check(data, path)) {
            return get(data).get(path).asList();
        }
        return defaultValue;
    }

    @ZenMethod
    public static long getLong(IData data, String path, @Optional long defaultValue) {
        if (check(data, path)) {
            return get(data).get(path).asLong();
        }
        return defaultValue;
    }

    @ZenMethod
    public static Map<String, IData> getMap(IData data, String path, @Optional Map<String, IData> defaultValue) {
        if (check(data, path)) {
            return get(data).get(path).asMap();
        }
        return defaultValue;
    }

    @ZenMethod
    public static short getShort(IData data, String path, @Optional short defaultValue) {
        if (check(data, path)) {
            return get(data).get(path).asShort();
        }
        return defaultValue;
    }

    @ZenMethod
    public static String getString(IData data, String path, @Optional String defaultValue) {
        if (check(data, path)) {
            return get(data).get(path).asString();
        }
        return defaultValue;
    }

    @ZenMethod
    public static byte[] getByteArray(IData data, String path, @Optional byte[] defaultValue) {
        if (check(data, path)) {
            return get(data).get(path).asByteArray();
        }
        return defaultValue;
    }

    @ZenMethod
    public static int[] getIntArray(IData data, String path, @Optional int[] defaultValue) {
        if (check(data, path)) {
            return get(data).get(path).asIntArray();
        }
        return defaultValue;
    }

    @ZenMethod
    public static boolean check(IData data, String... path) {
        final var nbt = get(data);
        for (String key : path) {
            if (!nbt.containsKey(key)) {
                return false;
            }
        }
        return true;
    }
}
