package github.kasuminova.novaeng.mixin.journeymap;

import codechicken.lib.model.bakery.IBakeryProvider;
import codechicken.lib.model.bakery.ModelErrorStateProperty;
import codechicken.lib.model.bakery.generation.IBlockBakery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.performance.JourneyMapStateMemo;
import journeymap.client.cartography.color.ColoredSprite;
import journeymap.client.mod.vanilla.VanillaBlockSpriteProxy;
import journeymap.client.model.block.BlockMD;
import journeymap.client.model.chunk.ChunkMD;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/**
 * Gives JourneyMap the same block state the world renderer bakes, so blocks whose model comes from a
 * CodeChicken Lib bakery keep their sprites on the map.
 *
 * <p>JourneyMap resolves block sprites by re-reading {@code world.getBlockState(pos)} and re-resolving the model from
 * it. That works for vanilla models, but a CodeChicken Lib bakery bakes from the <em>extended</em> block state:
 * {@code ModelBakery.handleExtendedState} is what calls the block's bakery, which is what fills in the sprite map and
 * the {@code ERROR_STATE} value. A state read straight out of the chunk has neither, so the library refuses to bake it -
 * the model turns into the missing model and every visit logs a FATAL - which is exactly what happened for Thermal
 * Expansion's machines, caches, cells, dynamos, ducts and lights, the blocks in this pack that implement
 * {@code IBakeryProvider}. Blocks without such a bakery are left untouched: extending every state here would make
 * JourneyMap's worker threads run every block's own {@code getExtendedState} on every map pass, which is not free for
 * blocks that build render contexts in theirs.</p>
 *
 * <p>The extended state is only handed over when the block's own {@code getExtendedState} reported success: a bakery
 * that cannot reach its block entity answers with an error state, and passing that on would trade this silence for a
 * per-tick log in the library while still not rendering anything, so those keep the state they were given.</p>
 *
 * <p>The result is remembered for the immediate repeat calls JourneyMap makes for the same position - it walks two
 * states by two facings per block, and each of those re-reads the same world state - keyed on the raw state instance so
 * a changed block is still re-extended.</p>
 *
 */
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

    @Unique
    private static IBlockState nova$extendState(final World world, final BlockPos pos, final IBlockState state) {
        final IBlockState extended;
        try {
            extended = state.getBlock().getExtendedState(state, world, pos);
        } catch (final Exception e) {
            if (!nova$reportedExtensionFailure) {
                nova$reportedExtensionFailure = true;
                NovaEngineeringCore.log.warn("Failed to build the extended block state of {} while resolving JourneyMap's block sprites; those blocks keep the state they were given.", state.getBlock().getRegistryName(), e);
            }
            return null;
        }

        if (extended == state || !(extended instanceof IExtendedBlockState ext)) {
            return null;
        }
        if (ext.getUnlistedProperties().containsKey(ModelErrorStateProperty.ERROR_STATE)) {
            final ModelErrorStateProperty.ErrorState errorState = ext.getValue(ModelErrorStateProperty.ERROR_STATE);
            if (errorState == null || errorState.hasErrored()) {
                return null;
            }
        }
        return extended;
    }

    @WrapOperation(
        method = "getSprites(Ljourneymap/client/model/block/BlockMD;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;Ljava/util/HashMap;Ljourneymap/client/model/chunk/ChunkMD;Lnet/minecraft/util/math/BlockPos;)Z",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;",
            remap = true),
        remap = false,
        require = 1
    )
    private IBlockState nova$extendStateForBakery(final World world, final BlockPos pos,
                                                  final Operation<IBlockState> original) {
        final IBlockState state = original.call(world, pos);
        final IBlockState extended = nova$bakeryState(world, pos, state);
        return extended != null ? extended : state;
    }

    @Unique
    private static IBlockState nova$bakeryState(final World world, final BlockPos pos, final IBlockState state) {
        if (state == null || pos == null) {
            return null;
        }
        final Block block = state.getBlock();
        if (!(block instanceof IBakeryProvider)) {
            return null;
        }
        if (!(((IBakeryProvider) block).getBakery() instanceof IBlockBakery)) {
            return null;
        }
        // The bakery reads the block entity out of the world, and this runs on a JourneyMap worker thread over chunks
        // it kept a reference to, which may have been unloaded since.
        if (!world.isBlockLoaded(pos)) {
            return null;
        }

        final JourneyMapStateMemo memo = JourneyMapStateMemo.current();
        if (memo != null && memo.matches(world, pos, state)) {
            return memo.result();
        }

        final IBlockState extended = nova$extendState(world, pos, state);
        if (memo != null) memo.remember(world, pos, state, extended);
        return extended;
    }

}
