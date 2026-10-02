package github.kasuminova.novaeng.mixin.journeymap;

import codechicken.lib.model.bakery.IBakeryProvider;
import codechicken.lib.model.bakery.ModelErrorStateProperty;
import codechicken.lib.model.bakery.generation.IBlockBakery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.NovaEngineeringCore;
import journeymap.client.mod.vanilla.VanillaBlockSpriteProxy;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

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
 * <p>{@code addSprites} also builds a {@code HashSet} and an {@code ArrayList} on every call, purely to drop the
 * duplicate quads of a list before reading their sprites off them, and throws both away when the loop ends. This runs
 * once per block of every chunk JourneyMap colours, so those two throwaways were a measurable share of what the map
 * task allocates. Both are handed out from a thread local and cleared for the next call instead, which is sound
 * because neither outlives the method: the deduplicated list is only read by the loop that follows it.</p>
 */
@Mixin(value = VanillaBlockSpriteProxy.class, remap = false)
public abstract class MixinVanillaBlockSpriteProxy {

    @Unique
    private static boolean nova$reportedExtensionFailure;

    @Unique
    private static final ThreadLocal<Object[]> nova$lastExtension = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<HashSet<BakedQuad>> nova$dedupeSet = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<ArrayList<BakedQuad>> nova$dedupeList = new ThreadLocal<>();

    @Redirect(
        method = "addSprites(Ljava/util/HashMap;Ljava/util/List;)Z",
        at = @At(value = "NEW", target = "(I)Ljava/util/HashSet;"),
        remap = false,
        require = 1
    )
    private HashSet<BakedQuad> nova$reuseDedupeSet(final int requestedSize) {
        if (!NovaEngCoreConfig.CLIENT.optimizeJourneymapSpriteDedupe) {
            return new HashSet<>(requestedSize);
        }
        HashSet<BakedQuad> set = nova$dedupeSet.get();
        if (set == null) {
            set = new HashSet<>();
            nova$dedupeSet.set(set);
        }
        set.clear();
        return set;
    }

    @Redirect(
        method = "addSprites(Ljava/util/HashMap;Ljava/util/List;)Z",
        at = @At(value = "NEW", target = "(Ljava/util/Collection;)Ljava/util/ArrayList;"),
        remap = false,
        require = 1
    )
    private ArrayList<BakedQuad> nova$reuseDedupeList(final Collection<?> source) {
        if (!NovaEngCoreConfig.CLIENT.optimizeJourneymapSpriteDedupe) {
            final ArrayList<BakedQuad> fresh = new ArrayList<>(source.size());
            for (final Object element : source) {
                fresh.add((BakedQuad) element);
            }
            return fresh;
        }
        ArrayList<BakedQuad> list = nova$dedupeList.get();
        if (list == null) {
            list = new ArrayList<>();
            nova$dedupeList.set(list);
        }
        list.clear();
        for (final Object element : source) {
            list.add((BakedQuad) element);
        }
        return list;
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
        if (!NovaEngCoreConfig.CLIENT.optimizeJourneymapBlockSprites) {
            return state;
        }
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

        final Object[] memo = nova$lastExtension.get();
        if (memo != null && memo[0] == world && pos.equals(memo[1]) && memo[2] == state) {
            return (IBlockState) memo[3];
        }

        final IBlockState extended = nova$extendState(world, pos, state);
        final Object[] next = memo == null ? new Object[4] : memo;
        next[0] = world;
        next[1] = pos;
        next[2] = state;
        next[3] = extended;
        if (memo == null) {
            nova$lastExtension.set(next);
        }
        return extended;
    }

    @Unique
    private static IBlockState nova$extendState(final World world, final BlockPos pos, final IBlockState state) {
        final IBlockState extended;
        try {
            extended = state.getBlock().getExtendedState(state, world, pos);
        } catch (final Exception e) {
            if (!nova$reportedExtensionFailure) {
                nova$reportedExtensionFailure = true;
                NovaEngineeringCore.log.warn("Failed to build the extended block state of "
                        + state.getBlock().getRegistryName()
                        + " while resolving JourneyMap's block sprites; those blocks keep the state they were given.", e);
            }
            return null;
        }

        if (extended == state || !(extended instanceof IExtendedBlockState)) {
            return null;
        }
        final IExtendedBlockState ext = (IExtendedBlockState) extended;
        if (ext.getUnlistedProperties().containsKey(ModelErrorStateProperty.ERROR_STATE)) {
            final ModelErrorStateProperty.ErrorState errorState = ext.getValue(ModelErrorStateProperty.ERROR_STATE);
            if (errorState == null || errorState.hasErrored()) {
                return null;
            }
        }
        return extended;
    }
}
