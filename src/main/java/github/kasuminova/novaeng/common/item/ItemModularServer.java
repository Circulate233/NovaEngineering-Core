package github.kasuminova.novaeng.common.item;

import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class ItemModularServer extends Item {

    public ItemModularServer(final String registryName) {
        setMaxStackSize(1);
        setCreativeTab(CreativeTabNovaEng.INSTANCE);
        setRegistryName(new ResourceLocation(Tags.MOD_ID, registryName)).setTranslationKey(Tags.MOD_ID + '.' + registryName);
    }

}
