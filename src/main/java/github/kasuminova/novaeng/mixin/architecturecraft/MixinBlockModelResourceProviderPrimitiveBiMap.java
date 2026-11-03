package github.kasuminova.novaeng.mixin.architecturecraft;

import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.resgen.BlockModelResourceProvider;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.resgen.ConcreteResourcePack;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.resgen.EnumResourceType;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.resgen.IResourceHolder;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.resgen.ResourceProvider;

import github.kasuminova.novaeng.client.resource.ModelMetadataAccess;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.InputStream;

@Mixin(value = BlockModelResourceProvider.class, remap = false)
public abstract class MixinBlockModelResourceProviderPrimitiveBiMap extends ResourceProvider {
    @Shadow
    private static String SIMPLE_BLOCK_MODEL;

    protected MixinBlockModelResourceProviderPrimitiveBiMap(ConcreteResourcePack resourcePack) {
        super(resourcePack);
    }

    /**
     * @author NovaEngineering
     * @reason Consume primitive model metadata without the legacy Integer-returning API.
     */
    @Overwrite
    public InputStream provide(String name) {
        ResourceLocation resource;
        String blockID = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf("."));
        Block blockFromLocation = Block.getBlockFromName((this.modID + ":" + blockID));
        String textureLocation = this.modID + ":blocks/" + blockID;
        int meta = ((ModelMetadataAccess) resourcePack).nova$getModelMetadata(name);
        if (blockFromLocation instanceof IResourceHolder
            && (resource =
            ((IResourceHolder) blockFromLocation)
                .getResource(EnumResourceType.TEXTURE, meta))
            != null) {
            textureLocation = resource.toString();
        }
        String simpleBlockJSON = SIMPLE_BLOCK_MODEL;
        simpleBlockJSON = simpleBlockJSON.replaceAll("%ALL%", textureLocation);
        return IOUtils.toInputStream(simpleBlockJSON);
    }
}
