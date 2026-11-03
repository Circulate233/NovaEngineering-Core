package github.kasuminova.novaeng.mixin.architecturecraft;

import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.common.ConcreteLog;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.resgen.ConcreteResourcePack;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.resgen.EnumResourceType;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.resgen.IResourceHolder;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.resgen.ItemModelResourceProvider;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.resgen.ResourceProvider;

import github.kasuminova.novaeng.client.resource.ModelMetadataAccess;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.io.InputStream;

@Mixin(value = ItemModelResourceProvider.class, remap = false)
public abstract class MixinItemModelResourceProviderPrimitiveBiMap extends ResourceProvider {
    @Shadow
    private static String SIMPLE_BLOCK_MODEL;
    @Shadow
    private static String SIMPLE_ITEM_MODEL;

    protected MixinItemModelResourceProviderPrimitiveBiMap(ConcreteResourcePack resourcePack) {
        super(resourcePack);
    }

    /**
     * @author NovaEngineering
     * @reason Consume primitive model metadata without the legacy Integer-returning API.
     */
    @Overwrite
    public InputStream provide(String name) {
        ResourceLocation resource;
        String itemID = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf("."));
        Item itemFromLocation = this.resourcePack.getItem(name);
        String textureLocation = this.modID + ":items/" + itemID;
        int meta = ((ModelMetadataAccess) resourcePack).nova$getModelMetadata(name);
        if (itemFromLocation instanceof IResourceHolder
            && (resource =
            ((IResourceHolder) itemFromLocation)
                .getResource(EnumResourceType.TEXTURE, meta))
            != null) {
            textureLocation = resource.toString();
        }
        if (Block.getBlockFromItem(itemFromLocation) != Blocks.AIR) {
            if (itemFromLocation.getRegistryName().getPath().equals(itemID)) {
                try {
                    String blockName = name.replace("/item/", "/block/");
                    if (this.resourcePack.realpackHasResourceName(blockName)) {
                        return this.resourcePack.getRealpackInputStreamByName(blockName);
                    }
                    return this.resourcePack.getInputStreamByName(blockName);
                } catch (IOException e) {
                    ConcreteLog.error("Failed to get item model for {}", name);
                }
            } else {
                String simpleBlockModel = SIMPLE_BLOCK_MODEL;
                simpleBlockModel = simpleBlockModel.replaceAll("%ALL%", textureLocation);
                return IOUtils.toInputStream(simpleBlockModel);
            }
        }
        String simpleItemModel = SIMPLE_ITEM_MODEL;
        simpleItemModel = simpleItemModel.replaceAll("%L0%", textureLocation);
        return IOUtils.toInputStream(simpleItemModel);
    }
}
