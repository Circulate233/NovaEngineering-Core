package github.kasuminova.novaeng.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.client.resource.ModelJsonMetadataIndex;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLFileResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;
import java.io.InputStream;

/** The second getInputStream opens the winning data file; the first opens its optional .mcmeta. */
@Mixin(FallbackResourceManager.class)
public abstract class MixinFallbackResourceManager {
    @WrapOperation(method = "getResource", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/resources/FallbackResourceManager;getInputStream(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/resources/IResourcePack;)Ljava/io/InputStream;",
        ordinal = 1), require = 1)
    private InputStream nova$openModelArchive(final FallbackResourceManager manager, final ResourceLocation location,
                                              final IResourcePack pack, final Operation<InputStream> original) throws IOException {
        final ModelJsonMetadataIndex index = ModelJsonMetadataIndex.instance();
        if (index.isQuerying(location) && (pack.getClass() == FileResourcePack.class || pack.getClass() == FMLFileResourcePack.class)) {
            return index.openArchive(location, pack, () -> original.call(manager, location, pack));
        }
        return original.call(manager, location, pack);
    }
}
