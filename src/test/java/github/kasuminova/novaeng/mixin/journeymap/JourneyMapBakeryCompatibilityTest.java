package github.kasuminova.novaeng.mixin.journeymap;

import codechicken.lib.model.bakery.CCBakeryModel;
import codechicken.lib.model.bakery.ModelErrorStateProperty;
import github.kasuminova.novaeng.common.performance.JourneyMapStateMemo;
import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import journeymap.api.services.ClientPlatformService;
import journeymap.client.model.chunk.ChunkMD;
import journeymap.common.helpers.ForgeClientHooks;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import static codechicken.lib.model.bakery.ModelErrorStateProperty.ERROR_STATE;
import static org.junit.jupiter.api.Assertions.*;

class JourneyMapBakeryCompatibilityTest {
    private static final BlockPos POS = new BlockPos(1, 64, 1);

    @BeforeAll
    static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    void appliesToInstalledJourneyMap() throws Exception {
        IsolatedMixinTransformer.transformInPackage("github.kasuminova.novaeng.mixin.journeymap",
            new String[]{"journeymap.client.mod.vanilla.VanillaBlockSpriteProxy"}, "MixinVanillaBlockSpriteProxy");
    }

    @Test
    void extendsOnceForMultipleFacesAndKeepsUnusableStatesOutOfCcl() throws Exception {
        final Method handler = MixinVanillaBlockSpriteProxy.class.getDeclaredMethod("nova$getQuads", ClientPlatformService.class,
            IBakedModel.class, IBlockState.class, EnumFacing.class, BlockPos.class, ChunkMD.class);
        handler.setAccessible(true);
        final var patch = new MixinVanillaBlockSpriteProxy() { };
        final var block = new BakeryBlock();
        final IBlockState raw = block.getDefaultState();
        final IBlockState ready = ((IExtendedBlockState) raw).withProperty(ERROR_STATE, ModelErrorStateProperty.ErrorState.OK);
        final var model = new CCBakeryModel();
        final var world = allocateWorld();
        final var chunk = new ChunkMD(new Chunk(world, 0, 0) {
            @Override public Random getRandomWithSeed(final long seed) { return new Random(seed); }
        }) {
            @Override public World getWorld() { return world; }
        };
        final var service = new ForgeClientHooks() {
            @Override
            public List<BakedQuad> getQuads(final IBakedModel m, final IBlockState state, final EnumFacing side, final BlockPos pos) {
                assertSame(ready, state);
                return List.of();
            }
        };
        final ClientPlatformService mustSkip = new ForgeClientHooks() {
            @Override
            public List<BakedQuad> getQuads(final IBakedModel m, final IBlockState state, final EnumFacing side, final BlockPos pos) {
                return fail("Invalid state reached the model");
            }
        };

        block.result = ready;
        try (final var memo = JourneyMapStateMemo.begin()) {
            handler.invoke(patch, service, model, raw, EnumFacing.UP, POS, chunk);
            handler.invoke(patch, service, model, raw, EnumFacing.NORTH, POS, chunk);
            assertEquals(1, block.extensions);
        }
        block.result = ((IExtendedBlockState) raw).withProperty(ERROR_STATE, ModelErrorStateProperty.ErrorState.of("no tile"));
        try (final var memo = JourneyMapStateMemo.begin()) {
            assertEquals(List.of(), handler.invoke(patch, mustSkip, model, raw, EnumFacing.UP, POS, chunk));
            assertEquals(List.of(), handler.invoke(patch, mustSkip, model, raw, EnumFacing.NORTH, POS, chunk));
            assertEquals(2, block.extensions);
        }
        assertEquals(List.of(), handler.invoke(patch, mustSkip, model, raw, EnumFacing.UP, POS, null));
        assertEquals(List.of(), handler.invoke(patch, mustSkip, model, null, EnumFacing.UP, POS, chunk));
    }

    private static TestWorld allocateWorld() throws Exception {
        final Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        final var field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (TestWorld) unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), TestWorld.class);
    }

    private static final class TestWorld extends World {
        TestWorld() { super(null, null, null, null, false); }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(final int x, final int z, final boolean allowEmpty) { return true; }
    }

    private static final class BakeryBlock extends Block {
        IBlockState result;
        int extensions;

        BakeryBlock() { super(Material.ROCK); }
        @Override
        protected BlockStateContainer createBlockState() {
            return new ExtendedBlockState(this, new IProperty<?>[0], new IUnlistedProperty<?>[]{ERROR_STATE});
        }
        @Override
        public IBlockState getExtendedState(final IBlockState state, final IBlockAccess world, final BlockPos pos) {
            extensions++;
            return result;
        }
    }
}
