package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.client.gl.FrustumCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips a tile entity renderer once its whole render volume sits further than the configured
 * distance from the camera. Only volumes that fit inside a fixed box are considered, so large
 * multiblock structures keep rendering from any distance; and infinite bounding boxes, which is
 * what Minecraft uses to mark a renderer as "may draw anywhere", are never skipped.
 *
 * <p>Tile entity renderers are pure visuals, so skipping one cannot change game state. The pass
 * and destroy-stage handling stay in the renderer, and the caller's batching is unaffected.</p>
 */
@Mixin(value = TileEntityRendererDispatcher.class, remap = false)
public abstract class MixinTileEntityRendererDistance {

    /** Render volumes at least this large on any axis are treated as structures and never skipped. */
    @Unique
    private static final double NOVA$MAX_GATED_EXTENT = 32.0D;

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;FI)V", at = @At("HEAD"),
            cancellable = true, remap = true, require = 1)
    private void nova$skipDistantRenderer(final TileEntity tileentityIn,
                                          final float partialTicks,
                                          final int destroyStage,
                                          final CallbackInfo ci) {
        if (!NovaEngCoreConfig.CLIENT.optimizeTesrRenderDistance) {
            return;
        }
        final int limit = NovaEngCoreConfig.CLIENT.tesrRenderDistance;
        if (limit <= 0) {
            return;
        }

        final Entity view = Minecraft.getMinecraft().getRenderViewEntity();
        if (view == null) {
            return;
        }

        // A render box encloses the tile entity's own position and is capped at NOVA$MAX_GATED_EXTENT per axis, so the
        // position alone decides both ends: inside the limit the box cannot reject it, and more than one cap past the
        // limit the box cannot reach back in. Asking for the box is not free - mods derive it from the world, Mekanism's
        // transmitters even re-read their tile entity and neighbours to answer - so only the narrow band in between pays
        // for it.
        final BlockPos pos = tileentityIn.getPos();
        final double px = nova$axisDistance(view.posX, pos.getX(), pos.getX());
        final double py = nova$axisDistance(view.posY, pos.getY(), pos.getY());
        final double pz = nova$axisDistance(view.posZ, pos.getZ(), pos.getZ());
        if (px * px + py * py + pz * pz <= (double) limit * (double) limit) {
            return;
        }
        final double reach = (double) limit + NOVA$MAX_GATED_EXTENT;
        if (px > reach || py > reach || pz > reach) {
            ci.cancel();
            return;
        }

        final AxisAlignedBB box = tileentityIn.getRenderBoundingBox();
        if (box == null || box == TileEntity.INFINITE_EXTENT_AABB) {
            return;
        }
        // The renderer only culls whole chunks, so a renderer in a visible chunk but behind the camera is still drawn
        // in full. A box entirely outside the view cannot contribute anything visible, and unlike the distance gate
        // this rejects nothing that would have been seen.
        if (NovaEngCoreConfig.CLIENT.optimizeTesrFrustumCulling && FrustumCache.isOutside(box)) {
            ci.cancel();
            return;
        }
        if (box.maxX - box.minX > NOVA$MAX_GATED_EXTENT
                || box.maxY - box.minY > NOVA$MAX_GATED_EXTENT
                || box.maxZ - box.minZ > NOVA$MAX_GATED_EXTENT) {
            return;
        }

        final double dx = nova$axisDistance(view.posX, box.minX, box.maxX);
        final double dy = nova$axisDistance(view.posY, box.minY, box.maxY);
        final double dz = nova$axisDistance(view.posZ, box.minZ, box.maxZ);
        if (dx * dx + dy * dy + dz * dz > (double) limit * (double) limit) {
            ci.cancel();
        }
    }

    @Unique
    private static double nova$axisDistance(final double value, final double min, final double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0.0D;
    }
}
