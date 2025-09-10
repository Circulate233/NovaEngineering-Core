package github.kasuminova.novaeng.common.util;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.novaeng.NovaEngineeringCore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.text.translation.I18n;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.List;

@ZenRegister
@ZenClass("novaeng.Function")
public class Function {

    @SafeVarargs
    public static <T> List<T> asList(T... s){
        return ObjectArrayList.wrap(s);
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
