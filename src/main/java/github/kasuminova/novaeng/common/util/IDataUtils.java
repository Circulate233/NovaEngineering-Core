package github.kasuminova.novaeng.common.util;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.data.IData;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.List;
import java.util.Map;

@ZenRegister
@ZenExpansion("crafttweaker.data.IData")
public class IDataUtils {

    private static NBTTagCompound get(IData data) {
        return get(data,"NULL");
    }

    private static NBTTagCompound get(IData data,String path) {
        var datan = CraftTweakerMC.getNBT(data);
        if (datan instanceof NBTTagCompound nt){
            return nt;
        }
        var nbt = new NBTTagCompound();
        nbt.setTag(path,datan);
        return nbt;
    }

    @ZenMethod
    public static IData get(IData data,String path,@Optional IData defaultValue) {
        if (check(data,path)){
            return CraftTweakerMC.getIData(get(data,path).getTag(path));
        }
        return defaultValue;
    }

    @ZenMethod
    public static boolean getBool(IData data, String path,@Optional boolean defaultValue){
        if (check(data,path)){
            return get(data,path).getBoolean(path);
        }
        return defaultValue;
    }

    @ZenMethod
    public static byte getByte(IData data, String path,@Optional byte defaultValue) {
        if (check(data,path)){
            return get(data,path).getByte(path);
        }
        return defaultValue;
    }

    @ZenMethod
    public static double getDouble(IData data, String path, @Optional double defaultValue) {
        if (check(data, path)) {
            return get(data,path).getDouble(path);
        }
        return defaultValue;
    }

    @ZenMethod
    public static float getFloat(IData data, String path, @Optional float defaultValue) {
        if (check(data, path)) {
            return get(data,path).getFloat(path);
        }
        return defaultValue;
    }

    @ZenMethod
    public static int getInt(IData data, String path, @Optional int defaultValue) {
        if (check(data, path)) {
            return get(data,path).getInteger(path);
        }
        return defaultValue;
    }

    @ZenMethod
    public static List<IData> getList(IData data, String path, @Optional List<IData> defaultValue) {
        if (check(data, path)) {
            return CraftTweakerMC.getIData(get(data,path).getTag(path)).asList();
        }
        return defaultValue;
    }

    @ZenMethod
    public static long getLong(IData data, String path, @Optional long defaultValue) {
        if (check(data, path)) {
            return get(data,path).getLong(path);
        }
        return defaultValue;
    }

    @ZenMethod
    public static Map<String, IData> getMap(IData data, String path, @Optional Map<String, IData> defaultValue) {
        if (check(data, path)) {
            return CraftTweakerMC.getIData(get(data,path).getTag(path)).asMap();
        }
        return defaultValue;
    }

    @ZenMethod
    public static short getShort(IData data, String path, @Optional short defaultValue) {
        if (check(data, path)) {
            return get(data,path).getShort(path);
        }
        return defaultValue;
    }

    @ZenMethod
    public static String getString(IData data, String path, @Optional String defaultValue) {
        if (check(data, path)) {
            return get(data,path).getString(path);
        }
        return defaultValue;
    }

    @ZenMethod
    public static boolean check(IData data, String... path){
        final var nbt = get(data);
        for (String key : path) {
            if (!nbt.hasKey(key)){
                return false;
            }
        }
        return true;
    }
}
