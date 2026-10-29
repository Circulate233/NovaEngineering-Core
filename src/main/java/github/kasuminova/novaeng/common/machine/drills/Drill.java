package github.kasuminova.novaeng.common.machine.drills;

import blusunrize.immersiveengineering.api.tool.ExcavatorHandler;
import blusunrize.immersiveengineering.api.tool.ExcavatorHandler.MineralMix;
import blusunrize.immersiveengineering.api.tool.ExcavatorHandler.MineralWorldInfo;
import blusunrize.immersiveengineering.common.Config;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.oredict.IOreDictEntry;
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent;
import github.kasuminova.mmce.common.event.machine.MachineStructureFormedEvent;
import github.kasuminova.mmce.common.event.machine.MachineStructureUpdateEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeStartEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeCheckEvent;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.crafttweaker.expansion.RecipePrimerHyperNet;
import github.kasuminova.novaeng.common.crafttweaker.hypernet.HyperNetHelper;
import github.kasuminova.novaeng.common.handler.OreHandler;
import github.kasuminova.novaeng.common.machine.MachineSpecial;
import github.kasuminova.novaeng.common.performance.DrillOutputLookup;
import github.kasuminova.novaeng.common.util.IDataUtils;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeModifierBuilder;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public abstract class Drill implements MachineSpecial {
    public static final IItemStack errorStone;
    public static final IItemStack stone = CraftTweakerAPI.itemUtils.getItem("minecraft:stone", 0);
    public static final IOreDictEntry circuit_0 = CraftTweakerAPI.oreDict.get("programmingCircuit");
    public static final IOreDictEntry dust = CraftTweakerAPI.oreDict.get("itemPulsatingPowder");
    protected static final int[] tqsz = {-1, 0, 1};
    protected static final Object2IntOpenHashMap<String> tqdzb = new Object2IntOpenHashMap<>();
    protected static int basicMineralMix;
    protected static int MMMineralMix;

    static {
        int index = 0;
        for (final int x : tqsz) {
            for (final int z : tqsz) {
                tqdzb.put("" + (x + 1) + (z + 1), ++index);
            }
        }
        basicMineralMix = Config.IEConfig.Machines.excavator_depletion;
        MMMineralMix = basicMineralMix * 3;
        final var item = new ItemStack(Blocks.STONE);
        final var tag = new NBTTagCompound();
        tag.setByte("error", (byte) 1);
        item.setTagCompound(tag);
        errorStone = CraftTweakerMC.getIItemStack(item);
    }

    private final ResourceLocation resourceLocation = new ResourceLocation(ModularMachinery.MODID, this.getMachineName());
    private final IIngredient[] emptyIngredient = {};

    protected static int chunkCoord(final int position) {
        return position >> 4;
    }

    private static MineralMix getUsableMix(final MineralWorldInfo info) {
        return info.mineralOverride == null ? info.mineral : info.mineralOverride;
    }

    private static IItemStack getOreOutput(final TileMultiblockMachineController controller, final BlockPos pos,
                                           final int dimension, final int x, final int z) {
        final int k = x + 1, kk = z + 1;
        final var world = DimensionManager.getWorld(dimension);
        if (world == null) {
            return errorStone.mutable().copy();
        }
        final var data = controller.getCustomDataTag();
        final byte research = data.getByte("research_progress"), components = data.getByte("components_amount");
        final double damage = Math.max((data.hasKey("damage_probability") ? data.getInteger("damage_probability") : 10000)
            - DrillOutputLookup.researchDamage(research), 0.0);
        final double parallelism = data.getInteger(DrillOutputLookup.parallelKey(k, kk))
            * DrillOutputLookup.componentMultiplier(components);
        int random = controller.getWorld().rand.nextInt(10000);
        final var info = ExcavatorHandler.getMineralWorldInfo(world, chunkCoord(pos.getX()) + x, chunkCoord(pos.getZ()) + z);
        if (damage < 10000) {
            if (damage > random) {
                info.depletion += (int) parallelism;
                data.setInteger(DrillOutputLookup.depletionKey(k, kk), info.depletion);
            }
        } else {
            final double multiplier = Math.floor(1.0F * damage) / 10000;
            info.depletion += (int) (parallelism * multiplier);
            if (damage - 10000 * multiplier > random) {
                info.depletion += (int) parallelism;
            }
            data.setInteger(DrillOutputLookup.depletionKey(k, kk), info.depletion);
        }
        final var mineral = getUsableMix(info);
        if (mineral == null) {
            data.setString(DrillOutputLookup.veinKey(k, kk), "empty");
            return stone;
        }
        final var ore = mineral.getRandomOre(world.rand);
        if (data.getByte("additional_component_raw_ore") == 1) {
            final var raw = OreHandler.getRawOre(ore);
            if (raw == null) {
                return OreHandler.getOre(ore);
            }
            random = world.rand.nextInt(6);
            return raw.amount(Math.max(random, 1));
        }
        return OreHandler.getOre(ore);
    }

    private static IItemStack getCrystalOutput(final TileMultiblockMachineController controller) {
        final int research = controller.getCustomDataTag().getByte("research_progress");
        if (research <= 0) {
            return stone;
        }
        final int index = controller.getWorld().rand.nextInt(DrillOutputLookup.crystalCount(research));
        return CraftTweakerAPI.itemUtils.getItem(DrillOutputLookup.crystalName(index), 0);
    }

    protected abstract String getCoreTheardName();

    protected abstract String getMachineName();

    protected abstract Type getType();

    protected abstract long getBaseEnergy();

    protected float getAdvancedRecipeTimeMultiple() {
        return 1;
    }

    protected float getRecipeTimeMultiple() {
        return 1;
    }

    protected boolean isDimensional() {
        return false;
    }

    protected IIngredient[] getExIngredient() {
        return this.emptyIngredient;
    }

    protected int getRecipeTime() {
        return (int) (120 * this.getRecipeTimeMultiple());
    }

    protected int getRecipeTime(final boolean advanced) {
        float time = 240 * this.getRecipeTimeMultiple();
        if (advanced) {
            time *= this.getAdvancedRecipeTimeMultiple();
        }
        return (int) time;
    }

    protected int getParallelism() {
        return this.getType() == Type.RANGE ? 64 : 4;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return this.resourceLocation;
    }

    @Override
    public void preInit(final DynamicMachine machine) {
        machine.addMachineEventHandler(MachineStructureUpdateEvent.class,
            event -> event.getController().setWorkMode(TileMultiblockMachineController.WorkMode.SEMI_SYNC));
        this.regUpgrade(machine);
        this.regRecipe(machine);
        if (NovaEngineeringCore.proxy.isClient()) {
            this.regGui(machine);
        }
    }

    private RecipePrimer addExInput(final RecipePrimer recipe) {
        if (this.getExIngredient().length != 0) {
            recipe.addInputs(this.getExIngredient());
        }
        return recipe;
    }

    private RecipePrimer addOutputs(final RecipePrimer recipe, final int count, final int x, final int z) {
        for (int i = 0; i < count; i++) {
            recipe.addOutput(stone);
            if (this.isDimensional()) {
                recipe.addItemModifier((controller, item) -> {
                    final int[] pos = controller.getController().getCustomDataTag().getIntArray("pos");
                    return getOreOutput(controller.getController(), new BlockPos(pos[0], pos[1], pos[2]), pos[3], x, z);
                });
            } else {
                recipe.addItemModifier((controller, item) -> getOreOutput(controller.getController(), controller.getController().getPos(),
                    controller.getIWorld().getDimension(), x, z));
            }
        }
        return recipe;
    }

    private boolean checkMineralMix(final RecipeCheckEvent event, final int k, final int kk) {
        final var controller = event.getController();
        final var data = controller.getCustomDataTag();
        final World world;
        if (this.isDimensional()) {
            if (!data.hasKey("pos")) {
                event.setFailed("novaeng.drill.failed.pos");
                return false;
            }
            world = DimensionManager.getWorld(data.getIntArray("pos")[3]);
        } else {
            world = controller.getWorld();
        }
        if (world == null) {
            event.setFailed("novaeng.drill.failed.mineral");
            return false;
        }
        if (data.getString(DrillOutputLookup.veinKey(k, kk)).equals("empty")) {
            event.setFailed("novaeng.drill.failed.mineral.empty");
            return false;
        }
        if (data.getInteger(DrillOutputLookup.depletionKey(k, kk)) >= MMMineralMix - 100) {
            event.setFailed("novaeng.drill.failed.mineral.depletion");
            data.setInteger(DrillOutputLookup.depletionKey(k, kk), MMMineralMix);
            data.setString(DrillOutputLookup.veinKey(k, kk), "empty");
        }
        return true;
    }

    private void initMineralMix(final World world, final int x, final int dx, final int z, final int dz, final NBTTagCompound data) {
        final String veinKey = DrillOutputLookup.veinKey(dx + 1, dz + 1);
        final var info = ExcavatorHandler.getMineralWorldInfo(world, chunkCoord(x) + dx, chunkCoord(z) + dz);
        final var mineral = getUsableMix(info);
        data.setString(veinKey, mineral == null ? "empty" : mineral.name);
        if (!data.getString(veinKey).equals("empty")) {
            data.setInteger(DrillOutputLookup.depletionKey(dx + 1, dz + 1), info.depletion);
        }
    }

    private void startWork(final FactoryRecipeStartEvent event, final int dx, final int dz, final int damage) {
        final var controller = event.getController();
        final var data = controller.getCustomDataTag();
        final int x, z;
        final World world;
        if (this.isDimensional()) {
            final int[] pos = data.getIntArray("pos");
            x = pos[0];
            z = pos[2];
            world = DimensionManager.getWorld(pos[3]);
        } else {
            x = controller.getPos().getX();
            z = controller.getPos().getZ();
            world = controller.getWorld();
        }
        this.initMineralMix(world, x, dx, z, dz, data);
        data.setInteger("damage_probability", damage);
        data.setInteger(DrillOutputLookup.parallelKey(dx + 1, dz + 1), event.getFactoryRecipeThread().getActiveRecipe().getParallelism());
    }

    private void regRecipe(final DynamicMachine machine) {
        if (this.getType() == Type.SINGLE) {
            final String threadName = this.getCoreTheardName();
            final var thread = FactoryRecipeThread.createCoreThread(threadName);
            machine.addCoreThread(thread);
            for (int variant = 0; variant < 2; variant++) {
                final String name = this.getMachineName() + (variant == 0 ? "_ex_01" : "_ex_11");
                final int damage = variant == 0 ? 8000 : 9000;
                final var recipe = RecipeBuilder.newBuilder(name, this.getMachineName(), this.getRecipeTime(), 1)
                                                .setLoadJEI(false).addEnergyPerTickInput(this.getBaseEnergy() * (variant == 0 ? 1 : 2))
                                                .addInput(variant == 0 ? circuit_0 : CraftTweakerAPI.itemUtils.getItem("thermalinnovation:drill", 4)).setChance(0)
                                                .addPreCheckHandler(event -> this.checkMineralMix(event, 1, 1))
                                                .addFactoryStartHandler(event -> this.startWork(event, 0, 0, damage));
                RecipePrimerHyperNet.requireComputationPoint(this.addOutputs(this.addExInput(recipe), 4, 0, 0), 1.5F)
                                    .addOutput(stone).addItemModifier((controller, item) -> getCrystalOutput(controller.getController()))
                                    .setChance(0.1F).setParallelized(variant != 0).setThreadName(threadName).build();
                thread.addRecipe(name);
            }
        } else {
            for (final int x : tqsz) {
                for (final int z : tqsz) {
                    final int k = x + 1, kk = z + 1;
                    final String threadName = this.getCoreTheardName() + "." + tqdzb.getInt("" + k + kk);
                    final var thread = FactoryRecipeThread.createCoreThread(threadName);
                    machine.addCoreThread(thread);
                    for (int variant = 0; variant < 2; variant++) {
                        final boolean advanced = variant != 0;
                        final String name = this.getMachineName() + (advanced ? "_ex_" : "") + k + kk;
                        final var recipe = RecipeBuilder.newBuilder(name, this.getMachineName(), this.getRecipeTime(advanced), 1)
                                                        .setLoadJEI(false).addEnergyPerTickInput(this.getBaseEnergy() * (advanced ? 2 : 1))
                                                        .addPreCheckHandler(event -> {
                                                            if (this.checkMineralMix(event, k, kk) && advanced) {
                                                                event.getActiveRecipe().setMaxParallelism(8);
                                                            }
                                                        })
                                                        .addFactoryStartHandler(event -> this.startWork(event, x, z, advanced ? 6000 : 8000))
                                                        .addInput(advanced ? dust : circuit_0).setChance(advanced ? 0.05F : 0);
                        RecipePrimerHyperNet.requireComputationPoint(this.addOutputs(recipe, 3, x, z), 3)
                                            .addOutput(stone).addItemModifier((controller, item) -> getCrystalOutput(controller.getController()))
                                            .setChance(advanced ? 0.04F : 0.035F).setMaxThreads(1).setParallelized(advanced).setThreadName(threadName).build();
                        thread.addRecipe(name);
                    }
                }
            }
        }
        machine.setInternalParallelism(this.getParallelism());
        HyperNetHelper.proxyMachineForHyperNet(this.getRegistryName());
        if (!this.isDimensional()) {
            machine.addMachineEventHandler(MachineStructureFormedEvent.class, event -> {
                final var controller = event.getController();
                final var pos = controller.getPos();
                if (this.getType() == Type.SINGLE) {
                    this.initMineralMix(controller.getWorld(), pos.getX(), 0, pos.getZ(), 0, controller.getCustomDataTag());
                } else {
                    for (final int x : tqsz) {
                        for (final int z : tqsz) {
                            this.initMineralMix(controller.getWorld(), pos.getX(), x, pos.getZ(), z, controller.getCustomDataTag());
                        }
                    }
                }
            });
        }
    }

    @SideOnly(Side.CLIENT)
    public ObjectArrayList<String> initGui(final NBTTagCompound data) {
        final var info = new ObjectArrayList<String>();
        info.add(I18n.format("top.drill.status") + "§6[" + I18n.format("top.drill.research_progress") + data.getByte("research_progress")
            + "|" + I18n.format("top.drill.components_amount") + data.getByte("components_amount") + "§6]");
        if (data.hasKey("additional_component_raw_ore")) {
            info.add(I18n.format("top.drill.components_raw_ore"));
        }
        return info;
    }

    @SideOnly(Side.CLIENT)
    private void regGui(final DynamicMachine machine) {
        machine.addMachineEventHandler(ControllerGUIRenderEvent.class, event -> {
            final var data = event.getController().getCustomDataTag();
            final var info = this.initGui(data);
            if (this.getType() == Type.SINGLE) {
                String vein = data.getString("veinName11");
                final String translated = I18n.format("desc.immersiveengineering.info.mineral." + vein);
                if (!translated.startsWith("desc")) {
                    vein = translated;
                }
                if (!vein.isEmpty() && !vein.equals("empty")) {
                    info.add(I18n.format("novaeng.drill.mineral.name") + vein);
                    info.add(I18n.format("novaeng.drill.mineral.depletion.s") + (MMMineralMix - data.getInteger("depletion11")));
                } else if (vein.isEmpty()) {
                    info.add(I18n.format("novaeng.drill.mineral.empyt.s"));
                }
                if (vein.isEmpty()) {
                    info.add(I18n.format("novaeng.drill.mineral.empyt"));
                }
            } else {
                for (final int x : tqsz) {
                    for (final int z : tqsz) {
                        final String suffix = "" + (x + 1) + (z + 1);
                        String vein = data.getString("veinName" + suffix);
                        final String translated = I18n.format("desc.immersiveengineering.info.mineral." + vein);
                        if (!translated.startsWith("desc")) {
                            vein = translated;
                        }
                        if (!vein.isEmpty() && !vein.equals("empty")) {
                            info.add(I18n.format("novaeng.drill.mineral.depletion.r", tqdzb.getInt(suffix), vein) + (MMMineralMix - data.getInteger("depletion" + suffix)));
                        } else if (vein.isEmpty()) {
                            info.add(I18n.format("novaeng.drill.mineral.empyt.r", tqdzb.getInt(suffix)));
                        }
                        if (vein.equals("empty")) {
                            info.add(I18n.format("novaeng.drill.mineral.empyt"));
                        }
                    }
                }
            }
            event.setExtraInfo(info.toArray(String[]::new));
        });
    }

    private void regUpgrade(final DynamicMachine machine) {
        final String threadName = "novaeng.drill.thread.up", name = this.getMachineName();
        final var thread = FactoryRecipeThread.createCoreThread(threadName);
        if (this.isDimensional()) {
            RecipeBuilder.newBuilder("excavatorzb" + name, name, 10).addInput(CraftTweakerAPI.itemUtils.getItem("contenttweaker:zbk", 0))
                         .setNBTChecker((controller, item) -> {
                             if (!IDataUtils.check(item.getTag(), "binding")) {
                                 return false;
                             }
                             final int[] pos = IDataUtils.getIntArray(item.getTag(), "pos", null);
                             if (pos == null) {
                                 return false;
                             }
                             controller.getController().getCustomDataTag().setIntArray("poss", pos);
                             return true;
                         }).addOutput(CraftTweakerAPI.itemUtils.getItem("contenttweaker:zbk", 0))
                         .addPreCheckHandler(event -> {
                             if (event.getController().isWorking()) {
                                 event.setFailed("novaeng.machine.failed.work");
                             }
                         })
                         .addFactoryStartHandler(event -> {
                             final var data = event.getController().getCustomDataTag();
                             final int[] pos = data.getIntArray("poss");
                             final var world = DimensionManager.getWorld(pos[3]);
                             if (this.getType() == Type.SINGLE) {
                                 this.initMineralMix(world, pos[0], 0, pos[2], 0, data);
                             } else {
                                 for (final int x : tqsz) {
                                     for (final int z : tqsz) {
                                         this.initMineralMix(world, pos[0], x, pos[2], z, data);
                                     }
                                 }
                             }
                             data.setIntArray("pos", pos);
                         }).setLoadJEI(false).setParallelized(false).setThreadName(threadName).build();
            thread.addRecipe("excavatorzb" + name);
        }
        for (int index = 0; index < 3; index++) {
            final int i = index;
            final String researchName = "research_mineral_utilization_" + name + "_" + i;
            final String componentName = "additional_component_loading_" + name + "_" + i;
            thread.addRecipe(researchName);
            thread.addRecipe(componentName);
            final var research = RecipeBuilder.newBuilder(researchName, name, 10)
                                              .addPreCheckHandler(event -> {
                                                  if (event.getController().getCustomDataTag().getBoolean("research_mineral_" + i)) {
                                                      event.setFailed("novaeng.machine.failed.work");
                                                  }
                                              }).addFactoryFinishHandler(event -> {
                    final var controller = event.getController();
                    final var data = controller.getCustomDataTag();
                    controller.addPermanentModifier("research" + i, RecipeModifierBuilder.create("modularmachinery:energy", "input",
                        (float) (1 + 0.2 * (i + 1)), 1, false).build());
                    data.setBoolean("research_mineral_" + i, true);
                    data.setByte("research_progress", (byte) (data.getByte("research_progress") + 1));
                });
            RecipePrimerHyperNet.requireResearch(research, "research_mineral_utilization_" + i)
                                .setParallelized(false).setThreadName(threadName).setLoadJEI(false).build();
            final var component = RecipeBuilder.newBuilder(componentName, name, 100, 1)
                                               .addItemInput(CraftTweakerAPI.itemUtils.getItem("contenttweaker:additional_component_" + i, 0))
                                               .addPreCheckHandler(event -> {
                                                   if (event.getController().getCustomDataTag().getBoolean("additional_component_" + i)) {
                                                       event.setFailed("novaeng.machine.failed.work");
                                                   }
                                               }).addFactoryFinishHandler(event -> {
                    final var controller = event.getController();
                    final var data = controller.getCustomDataTag();
                    final byte count = data.getByte("components_amount");
                    controller.addPermanentModifier("additional" + i, RecipeModifierBuilder.create("modularmachinery:energy", "input",
                        (float) (1 + 0.3 * (i + 1)), 1, false).build());
                    controller.addPermanentModifier("additionalout", RecipeModifierBuilder.create("modularmachinery:item", "output",
                        (float) (Math.pow(count + 1, 3) * 2), 1, false).build());
                    data.setBoolean("additional_component_" + i, true);
                    data.setByte("components_amount", (byte) (count + 1));
                });
            RecipePrimerHyperNet.requireResearch(component, "additional_component_loading_" + i)
                                .setThreadName(threadName).setParallelized(false).setLoadJEI(false).build();
        }
        thread.addRecipe("additional_component_loading_" + name + "_3");
        final var advanced = RecipeBuilder.newBuilder("additional_component_loading_" + name + "_3", name, 100, 1)
                                          .addItemInput(CraftTweakerAPI.itemUtils.getItem("contenttweaker:additional_component_3", 0))
                                          .addPreCheckHandler(event -> {
                                              if (event.getController().getCustomDataTag().getBoolean("additional_component_3")) {
                                                  event.setFailed("novaeng.machine.failed.work");
                                              }
                                          })
                                          .addFactoryFinishHandler(event -> {
                                              final var controller = event.getController();
                                              final var data = controller.getCustomDataTag();
                                              final byte count = data.getByte("components_amount"), research = data.getByte("research_progress");
                                              controller.addPermanentModifier("additional_ex", RecipeModifierBuilder.create("modularmachinery:energy", "input", 4, 1, false).build());
                                              controller.addPermanentModifier("additionalout", RecipeModifierBuilder.create("modularmachinery:item", "output", (float) (Math.pow(count + 1, 3) * 2), 1, false).build());
                                              data.setBoolean("additional_component_3", true);
                                              data.setByte("components_amount", (byte) (count + 1));
                                              data.setByte("research_progress", (byte) (research + 1));
                                          }).setThreadName(threadName).setParallelized(false).setLoadJEI(false);
        RecipePrimerHyperNet.requireResearch(advanced, "additional_component_loading_ex").build();
        thread.addRecipe("additional_component_loading_" + name + "_raw_ore");
        final var raw = RecipeBuilder.newBuilder("additional_component_loading_" + name + "_raw_ore", name, 100, 1)
                                     .addItemInput(CraftTweakerAPI.itemUtils.getItem("contenttweaker:additional_component_raw_ore", 0))
                                     .addPreCheckHandler(event -> {
                                         if (event.getController().getCustomDataTag().getBoolean("additional_component_raw_ore")) {
                                             event.setFailed("novaeng.machine.failed.work");
                                         }
                                     })
                                     .addFactoryFinishHandler(event -> {
                                         final var controller = event.getController();
                                         controller.getCustomDataTag().setBoolean("additional_component_raw_ore", true);
                                         controller.addPermanentModifier("additional_raw_ore", RecipeModifierBuilder.create("modularmachinery:energy", "input", 2, 1, false).build());
                                     });
        RecipePrimerHyperNet.requireResearch(raw, "additional_component_loading_raw_ore")
                            .setThreadName(threadName).setParallelized(false).setLoadJEI(false).build();
        machine.addCoreThread(thread);
        machine.setMaxThreads(0);
    }

    @Override
    public void onTOPInfo(final ProbeMode mode, final IProbeInfo info, final EntityPlayer player,
                          final IProbeHitData hit, final TileMultiblockMachineController controller) {
        final var data = controller.getCustomDataTag();
        MachineSpecial.newBox(info).text("{*top.drill.status*}  ")
                      .text("{*top.drill.research_progress*}" + data.getByte("research_progress") + "  ")
                      .text("{*top.drill.components_amount*}" + data.getByte("components_amount"));
    }

    public enum Type {SINGLE, RANGE}
}
