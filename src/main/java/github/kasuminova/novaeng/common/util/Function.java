package github.kasuminova.novaeng.common.util;

import github.kasuminova.novaeng.NovaEngineeringCore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.text.translation.I18n;

import java.util.List;

public class Function {

    @SafeVarargs
    public static <T> List<T> asList(T... s){
        return ObjectArrayList.wrap(s);
    }

    public static String getText(String key,Object... objs){
        if (NovaEngineeringCore.proxy.isClient()){
            return I18n.translateToLocalFormatted(key, objs);
        }
        return "";
    }
}
