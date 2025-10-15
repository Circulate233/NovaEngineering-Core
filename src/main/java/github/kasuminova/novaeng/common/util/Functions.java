package github.kasuminova.novaeng.common.util;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.novaeng.NovaEngineeringCore;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.chars.CharList;
import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import it.unimi.dsi.fastutil.chars.CharSet;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleOpenHashSet;
import it.unimi.dsi.fastutil.doubles.DoubleSet;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.floats.FloatOpenHashSet;
import it.unimi.dsi.fastutil.floats.FloatSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.util.text.translation.I18n;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("novaeng.Function")
public class Functions {

    @SafeVarargs
    public static <T> ObjectList<T> asList(T... s){
        return ObjectArrayList.wrap(s);
    }

    public static IntList asList(int... s){
        return IntArrayList.wrap(s);
    }

    public static LongList asList(long... s){
        return LongArrayList.wrap(s);
    }

    public static ShortList asList(short... s){
        return ShortArrayList.wrap(s);
    }

    public static FloatList asList(float... s){
        return FloatArrayList.wrap(s);
    }

    public static DoubleList asList(double... s){
        return DoubleArrayList.wrap(s);
    }

    public static CharList asList(char... s){
        return CharArrayList.wrap(s);
    }

    @SafeVarargs
    public static <T> ObjectSet<T> asSet(T... s){
        return new ObjectOpenHashSet<>(s);
    }

    public static IntSet asSet(int... s){
        return new IntOpenHashSet(s);
    }

    public static LongSet asSet(long... s){
        return new LongOpenHashSet(s);
    }

    public static ShortSet asSet(short... s){
        return new ShortOpenHashSet(s);
    }

    public static FloatSet asSet(float... s){
        return new FloatOpenHashSet(s);
    }

    public static DoubleSet asSet(double... s){
        return new DoubleOpenHashSet(s);
    }

    public static CharSet asSet(char... s){
        return new CharOpenHashSet(s);
    }

    @ZenMethod
    public static String getText(String key){
        if (NovaEngineeringCore.proxy.isClient()){
            return I18n.translateToLocal(key);
        }
        return key;
    }

    @ZenMethod
    public static String getText(String key,Object... objs){
        if (NovaEngineeringCore.proxy.isClient()){
            return I18n.translateToLocalFormatted(key, objs);
        }
        return key;
    }
}
