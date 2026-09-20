package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.client.texture.CloudColorUploadState;
import github.kasuminova.novaeng.client.texture.CloudColorUploadStateImpl;
import github.kasuminova.novaeng.client.texture.TextureGeneration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.world.World;
import net.minecraftforge.client.CloudRenderer;
import net.minecraftforge.client.resource.IResourceType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

@Mixin(value = CloudRenderer.class, remap = false)
public class MixinCloudRenderer {

    @Unique
    private final CloudColorUploadState nova$uploadState = new CloudColorUploadStateImpl();

    @Redirect(method = "render",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;updateDynamicTexture()V",
            remap = true),
        remap = false,
        require = 1
    )
    private void nova$uploadChangedCloudColor(final DynamicTexture texture) {
        final int[] pixels = texture.getTextureData();
        if (pixels == null || pixels.length != 1) {
            this.nova$uploadState.invalidate();
            texture.updateDynamicTexture();
            return;
        }

        final TextureGeneration generation = (TextureGeneration) texture;
        final long currentGeneration = generation.nova$getDeleteGeneration();
        final int currentArgb = pixels[0];
        final World currentWorld = Minecraft.getMinecraft().world;

        if (!this.nova$uploadState.shouldUpload(texture, currentGeneration, currentArgb, currentWorld)) {
            return;
        }

        texture.updateDynamicTexture();
        this.nova$uploadState.record(
            texture, generation.nova$getDeleteGeneration(), currentArgb, currentWorld);
    }

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"), remap = false, require = 1)
    private void nova$invalidateCloudColorOnReload(@Nonnull final IResourceManager resourceManager,
                                                   @Nonnull final Predicate<IResourceType> resourcePredicate,
                                                   final CallbackInfo ci) {
        this.nova$uploadState.invalidate();
    }
}
