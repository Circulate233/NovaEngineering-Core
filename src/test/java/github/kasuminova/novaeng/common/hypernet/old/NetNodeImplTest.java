package github.kasuminova.novaeng.common.hypernet.old;

import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.machine.RecipeThread;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetNodeImplTest {
    private static final double ONE_HUNDRED_T = 100_000_000_000_000D;

    private static NetNodeImpl node(final boolean remote, final NBTTagCompound data) throws Exception {
        TestController controller = allocate(TestController.class);
        controller.data = data;
        controller.running = true;
        controller.testWorld = allocate(TestWorld.class);
        var remoteField = World.class.getDeclaredField("isRemote");
        remoteField.setAccessible(true);
        remoteField.setBoolean(controller.testWorld, remote);
        return new NetNodeImpl(controller);
    }

    // Only fixture shells bypass constructors: production NetNodeImpl and the real NBT code run normally.
    // World and MMCE controller constructors otherwise need Minecraft's registries and world services.
    private static <T> T allocate(final Class<T> type) throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        var unsafeField = unsafeType.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        return type.cast(unsafeType.getMethod("allocateInstance", Class.class).invoke(unsafe, type));
    }

    @Test
    void serverRebuildsSavedConsumptionFromResumedThreads() throws Exception {
        NBTTagCompound saved = new NBTTagCompound();
        saved.setDouble("c", ONE_HUNDRED_T);
        NetNodeImpl node = node(false, saved);

        node.readNBT(saved);
        assertEquals(0D, node.getComputationPointConsumption());

        RecipeThread thread = new TestRecipeThread();
        node.updateRecipeConsumer(thread, ONE_HUNDRED_T);
        assertEquals(ONE_HUNDRED_T, node.getComputationPointConsumption());
        node.updateRecipeConsumer(thread, ONE_HUNDRED_T);
        assertEquals(ONE_HUNDRED_T, node.getComputationPointConsumption());
    }

    @Test
    void repeatedSaveAndReloadDoesNotAccumulateConsumption() throws Exception {
        NBTTagCompound saved = new NBTTagCompound();
        saved.setDouble("c", ONE_HUNDRED_T);

        for (int reload = 0; reload < 5; reload++) {
            NetNodeImpl node = node(false, saved);
            node.readNBT(saved);
            node.updateRecipeConsumer(new TestRecipeThread(), ONE_HUNDRED_T);
            node.writeNBT();

            assertEquals(ONE_HUNDRED_T, node.getComputationPointConsumption(), "reload " + reload);
            assertEquals(ONE_HUNDRED_T, saved.getDouble("c"), "saved reload " + reload);
            saved = saved.copy();
        }
    }

    @Test
    void rereadingSameNodeDiscardsOldThreadReferences() throws Exception {
        NBTTagCompound saved = new NBTTagCompound();
        saved.setDouble("c", ONE_HUNDRED_T);
        NetNodeImpl node = node(false, saved);
        RecipeThread oldThread = new TestRecipeThread();
        node.updateRecipeConsumer(oldThread, ONE_HUNDRED_T);

        node.readNBT(saved);
        assertTrue(node.recipeConsumers.isEmpty());
        assertEquals(0D, node.getComputationPointConsumption());

        node.updateRecipeConsumer(new TestRecipeThread(), ONE_HUNDRED_T);
        node.onRecipeFinished(oldThread);
        assertEquals(ONE_HUNDRED_T, node.getComputationPointConsumption());
    }

    @Test
    void threadUpdatesFinishesAndMachineStopKeepTotalConsistent() throws Exception {
        NetNodeImpl node = node(false, new NBTTagCompound());
        RecipeThread first = new TestRecipeThread();
        RecipeThread second = new TestRecipeThread();
        node.updateRecipeConsumer(first, ONE_HUNDRED_T);
        node.updateRecipeConsumer(second, ONE_HUNDRED_T);
        assertEquals(2D * ONE_HUNDRED_T, node.getComputationPointConsumption());

        // The caller passes the per-recipe requirement multiplied by its current parallelism.
        node.updateRecipeConsumer(first, 3D * ONE_HUNDRED_T);
        assertEquals(4D * ONE_HUNDRED_T, node.getComputationPointConsumption());
        node.onRecipeFinished(second);
        node.onRecipeFinished(second);
        assertEquals(3D * ONE_HUNDRED_T, node.getComputationPointConsumption());

        TestController controller = (TestController) node.getOwner();
        controller.running = false;
        node.onMachineTick();
        assertTrue(node.recipeConsumers.isEmpty());
        assertEquals(0D, node.getComputationPointConsumption());
    }

    @Test
    void clientRetainsSyncedConsumptionWithoutServerThreadReferences() throws Exception {
        NBTTagCompound synced = new NBTTagCompound();
        synced.setDouble("c", ONE_HUNDRED_T);
        NetNodeImpl node = node(true, synced);
        node.updateRecipeConsumer(new TestRecipeThread(), 3D * ONE_HUNDRED_T);

        node.readNBT(synced);
        assertTrue(node.recipeConsumers.isEmpty());
        assertEquals(ONE_HUNDRED_T, node.getComputationPointConsumption());

        synced.setDouble("c", 2D * ONE_HUNDRED_T);
        node.readNBT(synced);
        assertEquals(2D * ONE_HUNDRED_T, node.getComputationPointConsumption());
    }

    @Test
    void loadingBeforeWorldAssignmentAlsoDiscardsSavedConsumption() throws Exception {
        NBTTagCompound saved = new NBTTagCompound();
        saved.setDouble("c", ONE_HUNDRED_T);
        NetNodeImpl node = node(false, saved);
        ((TestController) node.getOwner()).testWorld = null;

        node.readNBT(saved);
        assertEquals(0D, node.getComputationPointConsumption());
        assertTrue(node.recipeConsumers.isEmpty());
    }

    @Test
    void connectionAndUnrelatedCustomDataSurviveReloadAndSave() throws Exception {
        NBTTagCompound saved = new NBTTagCompound();
        BlockPos centerPos = new BlockPos(123, 64, -456);
        saved.setLong("centerPos", centerPos.toLong());
        saved.setString("unrelated", "preserved");
        saved.setDouble("c", 2D * ONE_HUNDRED_T);
        NetNodeImpl node = node(false, saved);

        node.readNBT(saved);
        assertEquals(centerPos, node.centerPos);
        assertNull(node.getCenter());
        node.updateRecipeConsumer(new TestRecipeThread(), ONE_HUNDRED_T);
        node.writeNBT();

        assertEquals(centerPos.toLong(), saved.getLong("centerPos"));
        assertEquals("preserved", saved.getString("unrelated"));
        assertEquals(ONE_HUNDRED_T, saved.getDouble("c"));
    }

    private static final class TestController extends TileFactoryController {
        private NBTTagCompound data;
        private World testWorld;
        private boolean running;

        @Override
        public World getWorld() {
            return testWorld;
        }

        @Override
        public NBTTagCompound getCustomDataTag() {
            return data;
        }

        @Override
        public boolean isWorking() {
            return running;
        }

        @Override
        public int getTicksExisted() {
            return 1;
        }

        @Override
        public void doControllerTick() {
        }

        @Override
        public CraftingStatus getControllerStatus() {
            return null;
        }

        @Override
        public void setControllerStatus(final CraftingStatus status) {
        }

        @Override
        public void flushContextModifier() {
        }
    }

    private static final class TestWorld extends World {
        private TestWorld() {
            super(null, null, null, null, false);
            throw new AssertionError("Fixture must be allocated without a world constructor");
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(final int x, final int z, final boolean allowEmpty) {
            return false;
        }
    }

    private static final class TestRecipeThread extends RecipeThread {
        private TestRecipeThread() {
            super(null);
        }

        @Override
        public void fireStartedEvent() {
        }

        @Override
        public void fireFinishedEvent() {
        }

        @Override
        public void tryRestartRecipe() {
        }

        @Override
        protected void createRecipeSearchTask() {
        }
    }
}
