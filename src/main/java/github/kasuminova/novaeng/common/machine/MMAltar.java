package github.kasuminova.novaeng.common.machine;

import WayofTime.bloodmagic.altar.BloodAltar;
import WayofTime.bloodmagic.api.impl.BloodMagicAPI;
import WayofTime.bloodmagic.block.BlockLifeEssence;
import WayofTime.bloodmagic.core.RegistrarBloodMagicBlocks;
import WayofTime.bloodmagic.core.data.Binding;
import WayofTime.bloodmagic.ritual.IMasterRitualStone;
import WayofTime.bloodmagic.ritual.types.RitualWellOfSuffering;
import WayofTime.bloodmagic.tile.TileAltar;
import WayofTime.bloodmagic.util.helper.NetworkHelper;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.oredict.IOreDictEntry;
import github.kasuminova.mmce.common.concurrent.Sync;
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent;
import github.kasuminova.mmce.common.event.machine.MachineStructureUpdateEvent;
import github.kasuminova.mmce.common.event.machine.MachineTickEvent;
import github.kasuminova.novaeng.NovaEngineeringCore;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.modifier.MultiBlockModifierReplacement;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor;
import ink.ikx.mmce.common.utils.StackUtils;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import kport.modularmagic.common.tile.TileLifeEssenceProvider;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import static github.kasuminova.novaeng.common.util.Functions.asList;
import static github.kasuminova.novaeng.common.util.Functions.getText;

@ZenRegister
@ModOnly("bloodmagic")
@ZenClass("novaeng.MMAltar")
public final class MMAltar implements MachineSpecial {
    public static final MMAltar INSTANCE = new MMAltar();
    public static final int[] pdfw1 = {-6, -10, -6};
    public static final int[] pdfw2 = {6, 10, 6};
    public static final String MACHINEID = "mm_altar";
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, MACHINEID);
    public static final List<BlockPos> posSet1 = asList(new BlockPos(-3, -5, -3), new BlockPos(-3, -5, 3), new BlockPos(3, -5, -3), new BlockPos(3, -5, 3),
        new BlockPos(-3, -4, -3), new BlockPos(-3, -4, 3), new BlockPos(3, -4, -3), new BlockPos(3, -4, 3));
    public static final List<BlockPos> posSet2 = asList(new BlockPos(-5, -3, -5), new BlockPos(-5, -3, 5), new BlockPos(5, -3, -5), new BlockPos(5, -3, 5));
    public static final List<BlockPos> posSet3 = asList(new BlockPos(-11, -2, -11), new BlockPos(-11, -2, 11), new BlockPos(11, -2, -11), new BlockPos(11, -2, 11));
    private static final Map<TileMultiblockMachineController, WeakReference<TileAltar>> CACHE = new WeakHashMap<>();
    public static Block BLOCKSJ1 = getOtherModsBlock("contenttweaker", "crystalmatrixforcefieldcontrolblock");
    public static Block BLOCKSJ2 = getOtherModsBlock("contenttweaker", "fallenstarforcefieldcontrolblock");
    public static Block BLOCKSJ3 = getOtherModsBlock("contenttweaker", "universalforcefieldcontrolblock");

    private MMAltar() {
    }

    private static Block getOtherModsBlock(final String mod, final String name) {
        final var block = GameRegistry.findRegistry(Block.class).getValue(new ResourceLocation(mod, name));
        return block == null ? Blocks.AIR : block;
    }

    private static BlockArray buildModifierReplacementBlockArray(final Block block, final List<BlockPos> positions) {
        final var array = new BlockArray();
        final var descriptor = new IBlockStateDescriptor(block);
        for (final var position : positions) {
            array.addBlock(position, new BlockArray.BlockInformation(asList(descriptor)));
        }
        return array;
    }

    private static void finishCheck(final TileAltar altar, final TileMultiblockMachineController controller, final NBTTagCompound data) {
        byte level = (byte) getLevel(altar), upgrade = 0;
        final var machine = controller.getFoundMachine();
        if (machine != null) {
            for (final var replacement : machine.getMultiBlockModifiers()) {
                if (replacement.matches(controller)) {
                    switch (replacement.getModifierName()) {
                        case "upgrade1", "upgrade2" -> upgrade++;
                        case "upgrade3" -> {
                            upgrade = (byte) (upgrade + 2);
                            level++;
                        }
                    }
                }
            }
        }
        data.setByte("level", level);
        data.setByte("upgrade", upgrade);
    }

    public static boolean checkRitualWellOfSuffering(final NBTTagCompound data, final TileMultiblockMachineController controller) {
        if (data.hasKey("pos")) {
            final int[] coordinates = data.getIntArray("pos");
            final var pos = BlockPos.PooledMutableBlockPos.retain(coordinates[0], coordinates[1], coordinates[2]);
            final var tile = controller.getWorld().getTileEntity(pos);
            pos.release();
            if (tile instanceof IMasterRitualStone stone && stone.getCurrentRitual() instanceof RitualWellOfSuffering) {
                return true;
            }
            data.removeTag("pos");
        }
        final var origin = controller.getPos();
        final var pos = new BlockPos.MutableBlockPos();
        for (int x = origin.getX() + pdfw1[0]; x <= origin.getX() + pdfw2[0] + 1; x++) {
            final int value = origin.getY() + pdfw2[1];
            final int value1 = origin.getY() + pdfw1[1];
            for (int y = Math.clamp(value1, 0, 255); y <= Math.clamp(value, 0, 255); y++) {
                for (int z = origin.getZ() + pdfw1[2]; z <= origin.getZ() + pdfw2[2] + 1; z++) {
                    final var tile = controller.getWorld().getTileEntity(pos.setPos(x, y, z));
                    if (tile instanceof IMasterRitualStone stone && stone.getCurrentRitual() instanceof RitualWellOfSuffering) {
                        data.setIntArray("pos", new int[]{x, y, z});
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @ZenMethod
    public static void registerRecipe(final int need, final int total, final int tier, final IIngredient input, final IItemStack output) {
        final String name = input instanceof IItemStack item ? Objects.requireNonNull(CraftTweakerMC.getItem(item.getDefinition()).getRegistryName()).toString() + item.getMetadata()
            : input instanceof IOreDictEntry ore ? ore.getName() : Objects.requireNonNull(CraftTweakerMC.getItem(output.getDefinition()).getRegistryName()).toString() + output.getMetadata();
        RecipeBuilder.newBuilder(name, MACHINEID, total / need, 1000).addItemInputs(input).addPreCheckHandler(event -> {
                         final var controller = event.getController();
                         final var data = controller.getCustomDataTag();
                         final var altar = getAltar(controller);
                         final int blood = getNowBlood(altar), maximum = getMaxBlood(altar);
                         final double parallelism = Math.min(Math.pow(4, data.getByte("upgrade")), (double) maximum / need);
                         if (blood < need) {
                             event.setFailed("novaeng.mm_altar.failed.blood");
                             return;
                         }
                         if (data.getByte("level") < tier) {
                             event.setFailed("novaeng.mm_altar.failed.level");
                             return;
                         }
                         event.getActiveRecipe().setMaxParallelism((int) parallelism);
                     }).addFactoryStartHandler(event -> {
                         final var data = event.getController().getCustomDataTag();
                         data.setLong("progress", 0);
                         data.setLong("totalProgress", (long) event.getFactoryRecipeThread().getActiveRecipe().getParallelism() * total);
                     }).addFactoryPreTickHandler(event -> {
                         final var controller = event.getController();
                         final var data = controller.getCustomDataTag();
                         final var altar = getAltar(controller);
                         final int blood = getNowBlood(altar);
                         final long progress = data.getLong("progress"), totalProgress = data.getLong("totalProgress");
                         final float consumption = altar == null ? 0 : altar.getConsumptionMultiplier();
                         final var recipe = event.getFactoryRecipeThread().getActiveRecipe();
                         final int ticks = recipe.getTotalTick();
                         final int required = need * recipe.getParallelism();
                         if (blood > required) {
                             if (progress < totalProgress) {
                                 recipe.setTick(ticks / 2);
                                 event.preventProgressing(getText("novaeng.mm_altar.progress.0", totalProgress - progress));
                             }
                             if (blood <= (consumption + 1.0F) * required) {
                                 if (progress + blood <= totalProgress) {
                                     data.setLong("progress", progress + blood);
                                     setBlood(altar, 0);
                                 } else {
                                     data.setLong("progress", totalProgress);
                                     drainBlood(altar, (int) (totalProgress - progress));
                                     recipe.setTick(ticks);
                                 }
                             } else {
                                 if (progress + (1 + consumption) * required <= totalProgress) {
                                     data.setLong("progress", (long) (progress + (1 + consumption) * required));
                                     drainBlood(altar, (int) ((1.0F + consumption) * required));
                                 } else {
                                     data.setLong("progress", totalProgress);
                                     drainBlood(altar, (int) (totalProgress - progress));
                                     recipe.setTick(ticks);
                                 }
                             }
                         } else {
                             event.preventProgressing(getText("novaeng.mm_altar.progress.1", required));
                         }
                     }).addOutput(output).setThreadName("novaeng.mm_altar.thread.0")
                     .addRecipeTooltip(getText("novaeng.mm_altar.recipe.0", need), getText("novaeng.mm_altar.recipe.1", total),
                         getText("novaeng.mm_altar.recipe.2", tier), "novaeng.mm_altar.recipe.3", "novaeng.mm_altar.recipe.4").build();
    }

    public static synchronized TileAltar getAltar(final TileMultiblockMachineController controller) {
        final var reference = CACHE.get(controller);
        TileAltar altar = reference == null ? null : reference.get();
        if (altar != null && !altar.isInvalid()) {
            return altar;
        }
        final var tile = controller.getWorld().getTileEntity(controller.getPos().offset(EnumFacing.DOWN, 4));
        altar = tile instanceof TileAltar found ? found : null;
        if (altar != null) {
            CACHE.put(controller, new WeakReference<>(altar));
        } else {
            CACHE.remove(controller);
        }
        return altar;
    }

    public static synchronized void removeWorld(final World world) {
        CACHE.entrySet().removeIf(entry -> {
            final TileAltar altar = entry.getValue().get();
            return entry.getKey().getWorld() == world || altar == null || altar.getWorld() == world;
        });
    }

    public static synchronized void clearCache() {
        CACHE.clear();
    }

    private static int getLevel(final TileAltar altar) {
        return altar == null ? 0 : altar.getTier().ordinal() + 1;
    }

    private static int getNowBlood(final TileAltar altar) {
        final var blood = altar == null ? null : getBloodAltar(altar);
        return blood == null || blood.getFluid() == null ? 0 : blood.getFluid().amount;
    }

    public static void setBlood(final TileAltar altar, final int value) {
        final var blood = altar == null ? null : getBloodAltar(altar);
        if (blood == null) {
            return;
        }
        if (blood.getFluid() == null) {
            blood.setMainFluid(new FluidStack(BlockLifeEssence.getLifeEssence(), value));
        } else {
            blood.getFluid().amount = value;
        }
    }

    public static void addBlood(final TileAltar altar, final int value) {
        final var blood = altar == null ? null : getBloodAltar(altar);
        if (blood == null) {
            return;
        }
        if (blood.getFluid() == null) {
            blood.setMainFluid(new FluidStack(BlockLifeEssence.getLifeEssence(), value));
            return;
        }
        final var fluid = blood.getFluid();
        final int maximum = blood.getCapacity(), required = maximum - fluid.amount;
        fluid.amount = value >= required ? maximum : fluid.amount + value;
    }

    private static int drainBlood(final TileAltar altar, final int value) {
        final var blood = altar == null ? null : getBloodAltar(altar);
        if (blood == null || blood.getFluid() == null) {
            return 0;
        }
        final var fluid = blood.getFluid();
        final int previous = fluid.amount;
        fluid.amount = Math.max(0, fluid.amount - value);
        return previous - fluid.amount;
    }

    private static BloodAltar getBloodAltar(final TileAltar altar) {
        final var handler = altar.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
        return handler instanceof BloodAltar blood ? blood : null;
    }

    private static int getMaxBlood(final TileAltar altar) {
        return altar == null ? 0 : altar.getCapacity();
    }

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }

    @Override
    public void preInit(final DynamicMachine machine) {
        machine.addMachineEventHandler(MachineStructureUpdateEvent.class,
            event -> event.getController().setWorkMode(TileMultiblockMachineController.WorkMode.SEMI_SYNC));
        machine.addMachineEventHandler(MachineStructureUpdateEvent.class, event -> {
            final var controller = event.getController();
            final var data = controller.getCustomDataTag();
            final var altar = getAltar(controller);
            final var mutable = new BlockPos.MutableBlockPos();
            final var origin = controller.getPos();
            Sync.addSyncTask(() -> {
                short sacrifice = 0;
                for (final var position : controller.getFoundPattern().getPattern().keySet()) {
                    mutable.setPos(origin.getX() + position.getX(), origin.getY() + position.getY(), origin.getZ() + position.getZ());
                    final var state = controller.getWorld().getBlockState(mutable);
                    if (state.getBlock() == RegistrarBloodMagicBlocks.BLOOD_RUNE && state.getBlock().getMetaFromState(state) == 3) {
                        sacrifice++;
                    }
                }
                data.setShort("sacrifice", sacrifice);
                finishCheck(altar, controller, data);
            });
        });
        machine.addMachineEventHandler(MachineTickEvent.class, event -> {
            final var controller = event.getController();
            if (controller.getWorld().getTotalWorldTime() % 100 == 0) {
                finishCheck(getAltar(controller), controller, controller.getCustomDataTag());
            }
        });
        machine.getMultiBlockModifiers().add(new MultiBlockModifierReplacement("upgrade1", buildModifierReplacementBlockArray(BLOCKSJ1, posSet1), ObjectLists.emptyList(),
            asList(getText("novaeng.mm_altar.upgrade.0"), getText("novaeng.mm_altar.upgrade1") + BLOCKSJ1.getLocalizedName(), getText("novaeng.mm_altar.upgrade.1", 1)), StackUtils.getStackFromBlockState(BLOCKSJ1.getDefaultState())));
        machine.getMultiBlockModifiers().add(new MultiBlockModifierReplacement("upgrade2", buildModifierReplacementBlockArray(BLOCKSJ2, posSet2), ObjectLists.emptyList(),
            asList(getText("novaeng.mm_altar.upgrade.0"), getText("novaeng.mm_altar.upgrade2") + BLOCKSJ2.getLocalizedName(), getText("novaeng.mm_altar.upgrade.1", 1)), StackUtils.getStackFromBlockState(BLOCKSJ2.getDefaultState())));
        machine.getMultiBlockModifiers().add(new MultiBlockModifierReplacement("upgrade3", buildModifierReplacementBlockArray(BLOCKSJ3, posSet3), ObjectLists.emptyList(),
            asList(getText("novaeng.mm_altar.upgrade.0"), getText("novaeng.mm_altar.upgrade3.0") + BLOCKSJ3.getLocalizedName(), getText("novaeng.mm_altar.upgrade.1", 2), getText("novaeng.mm_altar.upgrade3.1")), StackUtils.getStackFromBlockState(BLOCKSJ3.getDefaultState())));
        final var recipes = BloodMagicAPI.INSTANCE.getRecipeRegistrar();
        recipes.removeBloodAltar(new ItemStack(Items.BUCKET));
        for (final var recipe : recipes.getAltarRecipes()) {
            registerRecipe(recipe.getConsumeRate(), recipe.getSyphon(), recipe.getMinimumTier().toInt(), CraftTweakerMC.getIIngredient(recipe.getInput()), CraftTweakerMC.getIItemStack(recipe.getOutput()));
        }
        RecipeBuilder.newBuilder("czshr", MACHINEID, 10).addItemInput(CraftTweakerAPI.itemUtils.getItem("bloodmagic:sacrificial_dagger", 1)).setChance(0)
                     .addFactoryFinishHandler(event -> addBlood(getAltar(event.getController()), Integer.MAX_VALUE))
                     .addRecipeTooltip("novaeng.mm_altar.recipe.8").setParallelized(false).setThreadName("novaeng.mm_altar.thread.1").build();
        RecipeBuilder.newBuilder("lhwl", MACHINEID, 10).addPreCheckHandler(event -> {
            final var controller = event.getController();
            final var altar = getAltar(controller);
            if (getNowBlood(altar) < 10000) {
                event.setFailed("novaeng.mm_altar.failed.0");
                return;
            }
            final var pos = BlockPos.PooledMutableBlockPos.retain(controller.getPos().getX(), controller.getPos().getY() - 1, controller.getPos().getZ());
            final var tile = controller.getWorld().getTileEntity(pos);
            pos.release();
            if (tile instanceof TileLifeEssenceProvider.Output output) {
                final var item = output.getInventory().getStackInSlot(0);
                if (item.isEmpty()) {
                    event.setFailed("novaeng.mm_altar.failed.1");
                    return;
                }
                final var binding = Binding.fromStack(item);
                if (binding == null) {
                    event.setFailed("novaeng.mm_altar.failed.1");
                    return;
                }
                final var network = NetworkHelper.getSoulNetwork(binding);
                if (network.getCurrentEssence() >= NetworkHelper.getMaximumForTier(NetworkHelper.getCurrentMaxOrb(network))) {
                    event.setFailed("novaeng.mm_altar.failed.2");
                }
            } else {
                event.setFailed("novaeng.mm_altar.failed.1");
            }
        }).addFactoryFinishHandler(event -> {
            final var controller = event.getController();
            final var altar = getAltar(controller);
            final var pos = BlockPos.PooledMutableBlockPos.retain(controller.getPos().getX(), controller.getPos().getY() - 1, controller.getPos().getZ());
            final var tile = controller.getWorld().getTileEntity(pos);
            pos.release();
            if (tile instanceof TileLifeEssenceProvider.Output output) {
                final var binding = Binding.fromStack(output.getInventory().getStackInSlot(0));
                if (binding == null) {
                    return;
                }
                final var network = NetworkHelper.getSoulNetwork(binding);
                network.setCurrentEssence(network.getCurrentEssence() + drainBlood(altar, 10000));
            }
        }).addRecipeTooltip("novaeng.mm_altar.recipe.6", "novaeng.mm_altar.recipe.7").setParallelized(false).setThreadName("novaeng.mm_altar.thread.2").build();
        RecipeBuilder.newBuilder("mm_altar_level_infinity_orb", MACHINEID, Integer.MAX_VALUE, 9999999, false)
                     .addItemInputs(CraftTweakerAPI.itemUtils.getItem("contenttweaker:universalforcefieldcontrolblock", 0).amount(4))
                     .addPreCheckHandler(event -> {
                         final var controller = event.getController();
                         if (getNowBlood(getAltar(controller)) < Integer.MAX_VALUE) {
                             event.setFailed("novaeng.mm_altar.failed.blood");
                             return;
                         }
                         if (controller.getCustomDataTag().getByte("level") < 7) {
                             event.setFailed("novaeng.mm_altar.failed.level");
                         }
                     }).addFactoryStartHandler(event -> {
                         final var data = event.getController().getCustomDataTag();
                         data.setLong("progress", 0);
                         data.setLong("totalProgress", 214748364700L);
                     }).addFactoryPreTickHandler(event -> {
                         final var controller = event.getController();
                         final var data = controller.getCustomDataTag();
                         final var altar = getAltar(controller);
                         final int blood = getNowBlood(altar);
                         final long progress = data.getLong("progress");
                         final var thread = event.getFactoryRecipeThread();
                         final int ticks = thread.getActiveRecipe().getTotalTick();
                         if (progress < 214748364700L) {
                             event.preventProgressing(getText("novaeng.mm_altar.progress.0", 214748364700L - progress));
                         }
                         if (blood == Integer.MAX_VALUE) {
                             if (progress + Integer.MAX_VALUE <= 214748364700L) {
                                 data.setLong("progress", progress + Integer.MAX_VALUE);
                                 setBlood(altar, 0);
                             } else {
                                 data.setLong("progress", 214748364700L);
                                 setBlood(altar, 0);
                                 thread.getActiveRecipe().setTick(ticks);
                             }
                         }
                     }).addOutput(CraftTweakerAPI.itemUtils.getItem("contenttweaker:level_infinity_orb", 0)).setThreadName("novaeng.mm_altar.thread.0")
                     .addRecipeTooltip(getText("novaeng.mm_altar.recipe.0", Integer.MAX_VALUE), getText("novaeng.mm_altar.recipe.1", 214748364700L),
                         getText("novaeng.mm_altar.recipe.2", 7), "novaeng.mm_altar.recipe.5").setParallelized(false).build();
        if (NovaEngineeringCore.proxy.isClient()) {
            this.clientInit(machine);
        }
    }

    @SideOnly(Side.CLIENT)
    private void clientInit(final DynamicMachine machine) {
        machine.addMachineEventHandler(ControllerGUIRenderEvent.class, event -> {
            final var controller = event.getController();
            final var altar = getAltar(controller);
            final var data = controller.getCustomDataTag();
            final boolean ritual = checkRitualWellOfSuffering(data, controller);
            final var info = asList(getText("gui.mm_altar.tooltip.0", getNowBlood(altar), getMaxBlood(altar)),
                getText("gui.mm_altar.tooltip.1", data.getByte("level"), data.getByte("upgrade")), getText("gui.mm_altar.tooltip.2") + getText("mmaltar.well_of_suffering." + ritual));
            if (!ritual) {
                info.add(getText("gui.mm_altar.tooltip.3"));
                info.add(getText("gui.mm_altar.tooltip.4"));
                info.add(getText("gui.mm_altar.tooltip.5"));
            }
            event.setExtraInfo(info.toArray(String[]::new));
        });
    }
}
