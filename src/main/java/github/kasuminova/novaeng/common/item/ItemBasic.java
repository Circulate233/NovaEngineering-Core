package github.kasuminova.novaeng.common.item;

import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import github.kasuminova.novaeng.common.enchantment.MagicBreaking;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import java.util.*;

public class ItemBasic extends Item {

    protected static Map<String,ItemBasic> map = new HashMap<>();

    public static List<String> NAMES = Arrays.asList(
            MagicBreaking.MAGICBREAKING.getId() + "_stone"
    );

    public ItemBasic(final String name) {
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
        this.setRegistryName(new ResourceLocation(NovaEngineeringCore.MOD_ID, name));
        this.setTranslationKey(NovaEngineeringCore.MOD_ID + '.' + name);
    }

    public static List<Item> getAllItem() {
        List<Item> ItemBasics = new LinkedList<>();
        for (String name : NAMES){
            final ItemBasic item = new ItemBasic(name);
            ItemBasics.add(item);
            map.put(name,item);
        }
        return ItemBasics;
    }

    public static ItemBasic getItem(String name) {
        return map.get(name);
    }
}
