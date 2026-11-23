package github.kasuminova.novaeng.mixin.journeymap;

import codechicken.lib.model.bakery.CCBakeryModel;
import codechicken.lib.model.bakery.ModelErrorStateProperty;
import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.performance.JourneyMapStateMemo;
import journeymap.api.services.ClientPlatformService;
import journeymap.client.cartography.color.ColoredSprite;
import journeymap.client.mod.vanilla.VanillaBlockSpriteProxy;
import journeymap.client.model.block.BlockMD;
import journeymap.client.model.chunk.ChunkMD;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;

@Mixin(value = VanillaBlockSpriteProxy.class, remap = false)
public abstract class MixinVanillaBlockSpriteProxy {
    @Unique
    private static boolean nova$reportedExtensionFailure;

    @Inject(method = "getSprites(Ljourneymap/client/model/block/BlockMD;Ljourneymap/client/model/chunk/ChunkMD;Lnet/minecraft/util/math/BlockPos;)Ljava/util/Collection;",
        at = @At("HEAD"), require = 1)
    private void nova$beginExtension(final BlockMD block, final ChunkMD chunk, final BlockPos pos,
                                     final CallbackInfoReturnable<Collection<ColoredSprite>> ci) {
        JourneyMapStateMemo.begin();
    }

    @Inject(method = "getSprites(Ljourneymap/client/model/block/BlockMD;Ljourneymap/client/model/chunk/ChunkMD;Lnet/minecraft/util/math/BlockPos;)Ljava/util/Collection;",
        at = @At("RETURN"), require = 1)
    private void nova$endExtension(final BlockMD block, final ChunkMD chunk, final BlockPos pos,
                                   final CallbackInfoReturnable<Collection<ColoredSprite>> ci) {
        JourneyMapStateMemo.current().close();
    }

    @Redirect(
        method = "getSprites(Ljourneymap/client/model/block/BlockMD;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;Ljava/util/HashMap;Ljourneymap/client/model/chunk/ChunkMD;Lnet/minecraft/util/math/BlockPos;)Z",
        at = @At(value = "INVOKE", target = "Ljourneymap/api/services/ClientPlatformService;getQuads(Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;Lnet/minecraft/util/math/BlockPos;)Ljava/util/List;"),
        remap = false,
        require = 1
    )
    private List<BakedQuad> nova$getQuads(final ClientPlatformService service, final IBakedModel model,
                                        final IBlockState state, final EnumFacing side, final BlockPos pos,
                                        @Local(argsOnly = true) final ChunkMD chunk) {
        if (state instanceof IExtendedBlockState extended) {
            final var error = extended.getUnlistedProperties().get(ModelErrorStateProperty.ERROR_STATE);
            if (error != null) {
                if (error.isEmpty()) {
                    final IBlockState resolved = nova$resolveBakeryState(chunk, pos, extended);
                    if (resolved == null) return List.of();
                    return service.getQuads(model, resolved, side, pos);
                }
                if (extended.getValue(ModelErrorStateProperty.ERROR_STATE).hasErrored()) return List.of();
            }
        } else if (model instanceof CCBakeryModel) {
            // JourneyMap retries with a null state. CCL requires an extended state to bake.
            return List.of();
        }
        return service.getQuads(model, state, side, pos);
    }

    @Unique
    private static IBlockState nova$resolveBakeryState(final ChunkMD chunk, final BlockPos pos, final IExtendedBlockState state) {
        if (chunk == null || pos == null) return null;
        final World world = chunk.getWorld();
        if (world == null) return null;

        final JourneyMapStateMemo memo = JourneyMapStateMemo.current();
        if (memo != null && memo.matches(world, pos, state)) return memo.result();

        // A retained JourneyMap chunk can outlive the live chunk needed by the bakery.
        final IBlockState resolved = world.isBlockLoaded(pos) ? nova$extendState(world, pos, state) : null;
        if (memo != null) memo.remember(world, pos, state, resolved);
        return resolved;
    }

    @Unique
    private static IBlockState nova$extendState(final World world, final BlockPos pos, final IExtendedBlockState state) {
        try {
            final IBlockState resolved = state.getBlock().getExtendedState(state, world, pos);
            if (resolved instanceof IExtendedBlockState extended) {
                final var error = extended.getUnlistedProperties().get(ModelErrorStateProperty.ERROR_STATE);
                if (error != null && error.isPresent() && !extended.getValue(ModelErrorStateProperty.ERROR_STATE).hasErrored()) return resolved;
            }
        } catch (final Exception failure) {
            if (!nova$reportedExtensionFailure) {
                nova$reportedExtensionFailure = true;
                NovaEngineeringCore.log.warn("Could not extend {} at {} for JourneyMap's block sprites.",
                    state.getBlock().getRegistryName(), pos, failure);
            }
        }
        return null;
    }
}
