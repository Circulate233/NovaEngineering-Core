package github.kasuminova.novaeng.common.item;

import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import github.kasuminova.novaeng.common.hypernet.computer.module.base.ServerModuleBase;
import github.kasuminova.novaeng.novaeng_core.Tags;
import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

@Getter
public class ItemServerModule extends Item {

    protected final ServerModuleBase<?> boundedModule;

    public ItemServerModule(final String registryName, final ServerModuleBase<?> boundedModule) {
        setCreativeTab(CreativeTabNovaEng.INSTANCE);
        setRegistryName(new ResourceLocation(Tags.MOD_ID, registryName)).setTranslationKey(Tags.MOD_ID + '.' + registryName);
        this.boundedModule = boundedModule;
    }

    public ItemServerModule(final String registryName) {
        this(registryName, null);
    }

}
