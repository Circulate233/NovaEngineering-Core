package github.kasuminova.novaeng.common.crafttweaker.util;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.data.IData;
import github.kasuminova.novaeng.common.util.IDataUtils;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.List;
import java.util.Map;

@ZenRegister
@ZenExpansion("crafttweaker.data.IData")
public class IDataUtilsCrt {

    @ZenMethod
    public static IData get(IData data, String path, @Optional IData defaultValue) {
        return IDataUtils.get(data, path, defaultValue);
    }

    @ZenMethod
    public static boolean getBool(IData data, String path, @Optional boolean defaultValue) {
        return IDataUtils.getBool(data, path, defaultValue);
    }

    @ZenMethod
    public static byte getByte(IData data, String path, @Optional byte defaultValue) {
        return IDataUtils.getByte(data, path, defaultValue);
    }

    @ZenMethod
    public static double getDouble(IData data, String path, @Optional double defaultValue) {
        return IDataUtils.getDouble(data, path, defaultValue);
    }

    @ZenMethod
    public static float getFloat(IData data, String path, @Optional float defaultValue) {
        return IDataUtils.getFloat(data, path, defaultValue);
    }

    @ZenMethod
    public static int getInt(IData data, String path, @Optional int defaultValue) {
        return IDataUtils.getInt(data, path, defaultValue);
    }

    @ZenMethod
    public static List<IData> getList(IData data, String path, @Optional List<IData> defaultValue) {
        return IDataUtils.getList(data, path, defaultValue);
    }

    @ZenMethod
    public static long getLong(IData data, String path, @Optional long defaultValue) {
        return IDataUtils.getLong(data, path, defaultValue);
    }

    @ZenMethod
    public static Map<String, IData> getMap(IData data, String path, @Optional Map<String, IData> defaultValue) {
        return IDataUtils.getMap(data, path, defaultValue);
    }

    @ZenMethod
    public static short getShort(IData data, String path, @Optional short defaultValue) {
        return IDataUtils.getShort(data, path, defaultValue);
    }

    @ZenMethod
    public static String getString(IData data, String path, @Optional String defaultValue) {
        return IDataUtils.getString(data, path, defaultValue);
    }

    @ZenMethod
    public static byte[] getByteArray(IData data, String path, @Optional byte[] defaultValue) {
        return IDataUtils.getByteArray(data, path, defaultValue);
    }

    @ZenMethod
    public static int[] getIntArray(IData data, String path, @Optional int[] defaultValue) {
        return IDataUtils.getIntArray(data, path, defaultValue);
    }

    @ZenMethod
    public static boolean check(IData data, String path) {
        return IDataUtils.check(data, path);
    }

    @ZenMethod
    public static boolean check(IData data, String[] path) {
        return IDataUtils.check(data, path);
    }
}