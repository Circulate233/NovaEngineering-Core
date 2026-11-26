package github.kasuminova.novaeng.mixin.circulation;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Pseudo
@Mixin(targets = "com.circulation.circulation_networks.client.render.RotatingModelRenderHelper", remap = false)
public abstract class MixinFullBrightDisplayListLookup {
    @Shadow @Final
    private static Map<ResourceLocation, Integer> FULL_BRIGHT_DISPLAY_LISTS;

    @Inject(method = "renderFullBrightDisplayList", at = @At("HEAD"), cancellable = true, require = 1)
    private static void nova$lookupBeforeCapturingBuilder(
        final Minecraft minecraft, final Tessellator tessellator, final BufferBuilder buffer,
        final IBlockAccess lightAccess, final ResourceLocation modelLocation, final IBakedModel model,
        final IBlockState state, final TileEntity tileEntity, final BlockRenderLayer renderLayer,
        final double blockX, final double blockY, final double blockZ, final CallbackInfo ci) {
        final Integer displayList = FULL_BRIGHT_DISPLAY_LISTS.get(modelLocation);
        if (displayList != null) {
            GlStateManager.callList(displayList);
            ci.cancel();
        }
        // Misses run the original computeIfAbsent, including its invalidation and reentrant behavior.
    }
}
