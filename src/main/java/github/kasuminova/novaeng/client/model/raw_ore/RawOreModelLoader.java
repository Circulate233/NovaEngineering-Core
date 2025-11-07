package github.kasuminova.novaeng.client.model.raw_ore;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.client.ClientProxy;
import github.kasuminova.novaeng.common.item.ItemRawOre;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class RawOreModelLoader implements ICustomModelLoader {

    final RawOreOverrideList overrideList = new RawOreOverrideList();

    @Override
    public boolean accepts(ResourceLocation modelLocation) {
        if (modelLocation.getNamespace().equals(NovaEngineeringCore.MOD_ID)) {
            var path = modelLocation.getPath();
            return path.contains("raw_") && !path.endsWith("_block") && !path.endsWith("_ore");
        }
        return false;
    }

    @Override
    public @NotNull IModel loadModel(@NotNull ResourceLocation location) {
        IResourceManager mngr = Minecraft.getMinecraft().getResourceManager();
        final boolean isBlock = location.getPath().contains("block");
        final String[] split = location.getPath().split("\\.")[0].split("/");
        final var s = split[split.length - 1].split("_",
            isBlock ? 3 : split[split.length - 1].contains("gem") ? 4 : 3);
        final String id = s[s.length - 1];
        final ItemRawOre.BlockRawOre block = ItemRawOre.getRawBlock(id);
        final ItemRawOre.Type type;
        final Item item;
        if (block != null) {
            if (isBlock) {
                type = ItemRawOre.Type.BLOCK;
                item = block.getItem();
            } else {
                ItemRawOre i = ItemRawOre.getRawOre(id);
                type = i.getType();
                item = i;
            }
        } else {
            return ModelLoaderRegistry.getMissingModel();
        }
        String name = isBlock ? "blocks/raw_block/" + split[split.length - 1] : "items/raw_ore/" + split[split.length - 1];
        try {
            mngr.getAllResources(NovaEngineeringCore.getRL("textures/" + name + ".png"));
        } catch (IOException e) {
            name = type.getDefR();
            ClientProxy.addColorRawOreItem(item);
            if (isBlock) ClientProxy.addColorRawOreBlock(block);
        }
        if (isBlock) {
            IModel model;
            try {
                model = ModelLoaderRegistry.getModel(NovaEngineeringCore.getRL("block/raw_block/raw_block"));
            } catch (Exception e) {
                return ModelLoaderRegistry.getMissingModel();
            }
            return model.retexture(ImmutableMap.of("all", NovaEngineeringCore.getRL(name).toString()));
        } else {
            return new ItemLayerModel(ImmutableList.of(NovaEngineeringCore.getRL(name)), overrideList);
        }
    }

    @Override
    public void onResourceManagerReload(@NotNull IResourceManager resourceManager) {

    }
}