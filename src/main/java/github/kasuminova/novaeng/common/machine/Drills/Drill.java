package github.kasuminova.novaeng.common.machine.Drills;

import blusunrize.immersiveengineering.api.tool.ExcavatorHandler;
import blusunrize.immersiveengineering.common.Config;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.oredict.IOreDictEntry;
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent;
import github.kasuminova.mmce.common.event.machine.MachineStructureFormedEvent;
import github.kasuminova.mmce.common.event.machine.MachineStructureUpdateEvent;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.crafttweaker.hypernet.HyperNetHelper;
import github.kasuminova.novaeng.common.handler.OreHandler;
import github.kasuminova.novaeng.common.machine.MachineSpecial;
import github.kasuminova.novaeng.common.util.IDataUtils;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeModifierBuilder;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.val;
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

import javax.annotation.Nullable;
import java.util.List;

import static crafttweaker.CraftTweakerAPI.itemUtils;
import static crafttweaker.CraftTweakerAPI.oreDict;
import static github.kasuminova.novaeng.common.crafttweaker.expansion.RecipePrimerHyperNet.requireComputationPoint;
import static github.kasuminova.novaeng.common.crafttweaker.expansion.RecipePrimerHyperNet.requireResearch;
import static net.minecraft.util.text.translation.I18n.translateToLocalFormatted;

public abstract class Drill implements MachineSpecial {
    public final ResourceLocation REGISTRY_NAME;

    protected static final int[] tqsz = {-1, 0, 1};
    protected static final Object2IntMap<String> tqdzb = new Object2IntOpenHashMap<>();
    protected static int basicMineralMix;
    protected static int MMMineralMix;
    static final IItemStack errorStone;
    static final IItemStack stone = itemUtils.getItem("minecraft:stone", 0);
    static final IOreDictEntry circuit_0 = oreDict.get("programmingCircuit");
    static final IOreDictEntry dust = oreDict.get("itemPulsatingPowder");

    static {
        int j = 0;
        for (int i : tqsz) {
            for (int ii : tqsz) {
                tqdzb.put("" + (i + 1) + (ii + 1), ++j);
            }
        }
        basicMineralMix = Config.IEConfig.Machines.excavator_depletion;
        MMMineralMix = basicMineralMix * 3;

        var item = new ItemStack(Blocks.STONE);
        var nbt = new NBTTagCompound();
        nbt.setByte("error", (byte) 1);
        item.setTagCompound(nbt);
        errorStone = CraftTweakerMC.getIItemStack(item);
    }

    protected Drill() {
        REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, getMachineName());
    }

    @Override
    public void preInit(final DynamicMachine machine) {
        machine.addMachineEventHandler(MachineStructureUpdateEvent.class, event -> {
            TileMultiblockMachineController controller = event.getController();
            controller.setWorkMode(TileMultiblockMachineController.WorkMode.SEMI_SYNC);
        });
        regUpgrade(machine);
        regRecipe(machine);
        if (NovaEngineeringCore.proxy.isClient()) regGui(machine);
    }

    protected abstract String getCoreTheardName();

    protected int getRecipeTime() {
        return (int) (120 * getRecipeTimeMultiple());
    }

    protected int getRecipeTime(boolean isAdvanced) {
        var out = 240 * getRecipeTimeMultiple();
        if (isAdvanced) out *= getAdvancedRecipeTimeMultiple();
        return (int) out;
    }

    protected float getAdvancedRecipeTimeMultiple() {
        return 1;
    }

    protected float getRecipeTimeMultiple() {
        return 1;
    }

    protected abstract String getMachineName();

    protected abstract Type getType();

    protected abstract long getBaseEnergy();

    protected int getParallelism() {
        return switch (getType()) {
            case RANGE -> 64;
            case SINGLE -> 4;
        };
    }

    protected boolean isDimensional() {
        return false;
    }

    protected IIngredient[] getExIngredient() {
        return new IIngredient[0];
    }

    @Override
    public final ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }

    private void regRecipe(final DynamicMachine machine) {
        switch (getType()) {
            case SINGLE -> {
                var threadName = getCoreTheardName();
                var thread = FactoryRecipeThread.createCoreThread(threadName);
                machine.addCoreThread(thread);
                String recipeName;
                var r0 = RecipeBuilder.newBuilder(recipeName = (getMachineName() + "_ex_01"), getMachineName(), getRecipeTime(), 1)
                        .setLoadJEI(false)
                        .addEnergyPerTickInput(getBaseEnergy())
                        .addInput(circuit_0).setChance(0)
                        .addPreCheckHandler(event -> {
                            var ctrl = event.getController();
                            var data = ctrl.getCustomDataTag();
                            if (!data.hasKey("pos")) {
                                event.setFailed("novaeng.drill.failed.pos");
                                return;
                            }
                            World world;
                            if (isDimensional()) {
                                var poss = data.getIntArray("pos");
                                world = DimensionManager.getWorld(poss[3]);
                            } else {
                                world = ctrl.getWorld();
                            }
                            if (world == null) {
                                event.setFailed("novaeng.drill.failed.mineral");
                                return;
                            }
                            var kmm = data.getString("kmm11");
                            var depletion = data.getInteger("depletion11");
                            if (kmm.equals("empty")) {
                                event.setFailed("novaeng.drill.failed.mineral.empty");
                                return;
                            }
                            if (depletion >= (MMMineralMix - 100)) {
                                event.setFailed("novaeng.drill.failed.mineral.depletion");
                                data.setInteger("depletion11", MMMineralMix);
                                data.setString("kmm11", "empty");
                                return;
                            }
                            ctrl.setCustomDataTag(data);
                        })
                        .addFactoryStartHandler(event -> {
                            var ctrl = event.getController();
                            var data = ctrl.getCustomDataTag();
                            var kmm = data.getString("kmm11");
                            int x;
                            int z;
                            World world;
                            if (isDimensional()) {
                                var poss = data.getIntArray("pos");
                                x = poss[0];
                                z = poss[2];
                                world = DimensionManager.getWorld(poss[3]);
                            } else {
                                x = ctrl.getPos().getX();
                                z = ctrl.getPos().getZ();
                                world = ctrl.getWorld();
                            }
                            var bxs = event.getFactoryRecipeThread().getActiveRecipe().getParallelism();
                            var worldInfo = ExcavatorHandler.getMineralWorldInfo(
                                    world,
                                    chunkCoord(x),
                                    chunkCoord(z)
                            );
                            var mineral = getUsableMix(worldInfo);
                            if (mineral != null) {
                                if (!kmm.equals(mineral.name)) {
                                    data.setString("kmm11", mineral.name);
                                }
                            } else {
                                data.setString("kmm11", "empty");
                            }
                            data.setInteger("bxs11", bxs);
                            data.setInteger("sfsh", 8000);
                        });
                if (getExIngredient().length != 0) {
                    r0.addInputs(getExIngredient());
                }
                for (int i = 0; i < 4; i++) {
                    r0.addOutput(stone)
                            .addItemModifier((ctrl, item) -> {
                                if (isDimensional()) {
                                    var poss = ctrl.getController().getCustomDataTag().getIntArray("pos");
                                    var pos = new BlockPos(poss[0], poss[1], poss[2]);
                                    return getOreOutput(ctrl.getController(), pos, poss[3]);
                                } else {
                                    return getOreOutput(ctrl.getController(), ctrl.getController().getPos(), ctrl.getIWorld().getDimension());
                                }
                            });
                }
                requireComputationPoint(r0, 1.5f);
                r0.addOutput(stone).addItemModifier((ctrl, item) -> getCcrystalOutput(ctrl.getController())).setChance(0.1f)
                        .setParallelized(false)
                        .setThreadName(threadName)
                        .build();
                thread.addRecipe(recipeName);

                var r1 = RecipeBuilder.newBuilder(recipeName = (getMachineName() + "_ex_11"), getMachineName(), getRecipeTime(), 1)
                        .setLoadJEI(false)
                        .addEnergyPerTickInput(getBaseEnergy() * 2)
                        .addInput(itemUtils.getItem("thermalinnovation:drill", 4)).setChance(0)
                        .addPreCheckHandler(event -> {
                            var ctrl = event.getController();
                            var data = ctrl.getCustomDataTag();
                            if (!data.hasKey("pos")) {
                                event.setFailed("novaeng.drill.failed.pos");
                                return;
                            }
                            World world;
                            if (isDimensional()) {
                                var poss = data.getIntArray("pos");
                                world = DimensionManager.getWorld(poss[3]);
                            } else {
                                world = ctrl.getWorld();
                            }
                            if (world == null) {
                                event.setFailed("novaeng.drill.failed.mineral");
                                return;
                            }
                            var kmm = data.getString("kmm11");
                            var depletion = data.getInteger("depletion11");
                            if (kmm.equals("empty")) {
                                event.setFailed("novaeng.drill.failed.mineral.empty");
                                return;
                            }
                            if (depletion >= (MMMineralMix - 100)) {
                                event.setFailed("novaeng.drill.failed.mineral.depletion");
                                data.setInteger("depletion11", MMMineralMix);
                                data.setString("kmm11", "empty");
                                return;
                            }
                            ctrl.setCustomDataTag(data);
                        })
                        .addFactoryStartHandler(event -> {
                            var ctrl = event.getController();
                            var data = ctrl.getCustomDataTag();
                            var kmm = data.getString("kmm11");
                            int x;
                            int z;
                            World world;
                            if (isDimensional()) {
                                var poss = data.getIntArray("pos");
                                x = poss[0];
                                z = poss[2];
                                world = DimensionManager.getWorld(poss[3]);
                            } else {
                                x = ctrl.getPos().getX();
                                z = ctrl.getPos().getZ();
                                world = ctrl.getWorld();
                            }
                            var bxs = event.getFactoryRecipeThread().getActiveRecipe().getParallelism();
                            var worldInfo = ExcavatorHandler.getMineralWorldInfo(
                                    world,
                                    chunkCoord(x),
                                    chunkCoord(z)
                            );
                            var mineral = getUsableMix(worldInfo);
                            if (mineral != null) {
                                if (!kmm.equals(mineral.name)) {
                                    data.setString("kmm11", mineral.name);
                                }
                            } else {
                                data.setString("kmm11", "empty");
                            }
                            data.setInteger("bxs11", bxs);
                            data.setInteger("sfsh", 9000);
                        });
                if (getExIngredient().length != 0) {
                    r1.addInputs(getExIngredient());
                }
                for (int i = 0; i < 4; i++) {
                    r1.addOutput(stone)
                            .addItemModifier((ctrl, item) -> {
                                if (isDimensional()) {
                                    var poss = ctrl.getController().getCustomDataTag().getIntArray("pos");
                                    var pos = new BlockPos(poss[0], poss[1], poss[2]);
                                    return getOreOutput(ctrl.getController(), pos, poss[3]);
                                } else {
                                    return getOreOutput(ctrl.getController(), ctrl.getController().getPos(), ctrl.getIWorld().getDimension());
                                }
                            });
                }
                requireComputationPoint(r1, 1.5f);
                r1.addOutput(stone).addItemModifier((ctrl, item) -> getCcrystalOutput(ctrl.getController())).setChance(0.1f)
                        .setParallelized(true)
                        .setThreadName(threadName)
                        .build();
                thread.addRecipe(recipeName);
            }
            case RANGE -> {
                for (int i : tqsz) {
                    var k = i + 1;
                    for (int ii : tqsz) {
                        var kk = ii + 1;
                        var threadName = getCoreTheardName() + "." + tqdzb.getInt(String.valueOf(k) + kk);
                        var thread = FactoryRecipeThread.createCoreThread(threadName);
                        machine.addCoreThread(thread);
                        String recipeName;

                        var r0 = RecipeBuilder.newBuilder(recipeName = (getMachineName() + k + kk), getMachineName(), getRecipeTime(false), 1)
                                .setLoadJEI(false)
                                .addEnergyPerTickInput(getBaseEnergy())
                                .addPreCheckHandler(event -> {
                                    var ctrl = event.getController();
                                    var data = ctrl.getCustomDataTag();
                                    var kmm = data.getString("kmm" + k + kk);
                                    var depletion = data.getInteger("depletion" + k + kk);
                                    if (kmm.equals("empty")) {
                                        event.setFailed("novaeng.drill.failed.mineral.empty");
                                        return;
                                    }
                                    if (depletion >= (MMMineralMix - 100)) {
                                        event.setFailed("novaeng.drill.failed.mineral.depletion");
                                        data.setInteger("depletion" + k + kk, MMMineralMix);
                                        data.setString("kmm" + k + kk, "empty");
                                    }
                                })
                                .addFactoryStartHandler(event -> {
                                    var ctrl = event.getController();
                                    var data = ctrl.getCustomDataTag();
                                    data.getString("kmm" + k + kk);
                                    int x;
                                    int z;
                                    World world;
                                    if (isDimensional()) {
                                        var poss = data.getIntArray("pos");
                                        x = poss[0];
                                        z = poss[2];
                                        world = DimensionManager.getWorld(poss[3]);
                                    } else {
                                        x = ctrl.getPos().getX();
                                        z = ctrl.getPos().getZ();
                                        world = ctrl.getWorld();
                                    }
                                    var bxs = event.getFactoryRecipeThread().getActiveRecipe().getParallelism();
                                    var worldInfo = ExcavatorHandler.getMineralWorldInfo(
                                            world,
                                            (chunkCoord(x) + i),
                                            (chunkCoord(z) + ii)
                                    );
                                    var mineral = getUsableMix(worldInfo);
                                    if (mineral != null) {
                                        data.setString("kmm" + k + kk, mineral.name);
                                    }
                                    data.setInteger("sfsh", 8000);
                                    data.setInteger("bxs" + k + kk, bxs);
                                })
                                .addInput(circuit_0).setChance(0);
                        for (int j = 0; j < 3; j++) {
                            r0.addOutput(stone)
                                    .addItemModifier((ctrl, item) -> {
                                        if (isDimensional()) {
                                            var poss = ctrl.getController().getCustomDataTag().getIntArray("pos");
                                            var pos = new BlockPos(poss[0], poss[1], poss[2]);
                                            return getOreOutput(ctrl.getController(), pos, poss[3]);
                                        } else {
                                            return getOreOutput(ctrl.getController(), ctrl.getController().getPos(), ctrl.getIWorld().getDimension());
                                        }
                                    });
                        }

                        requireComputationPoint(r0, 3);
                        r0.addOutput(stone).addItemModifier((ctrl, item) -> getCcrystalOutput(ctrl.getController())).setChance(0.035f)
                                .setMaxThreads(1)
                                .setParallelized(false)
                                .setThreadName(threadName)
                                .build();
                        thread.addRecipe(recipeName);

                        var r1 = RecipeBuilder.newBuilder(recipeName = (getMachineName() + "_ex_" + k + kk), getMachineName(), getRecipeTime(true), 1)
                                .setLoadJEI(false)
                                .addEnergyPerTickInput(getBaseEnergy() * 2)
                                .addPreCheckHandler(event -> {
                                    var ctrl = event.getController();
                                    var data = ctrl.getCustomDataTag();
                                    var kmm = data.getString("kmm" + k + kk);
                                    var depletion = data.getInteger("depletion" + k + kk);
                                    if (kmm.equals("empty")) {
                                        event.setFailed("novaeng.drill.failed.mineral.empty");
                                        return;
                                    }
                                    if (depletion >= (MMMineralMix - 100)) {
                                        event.setFailed("novaeng.drill.failed.mineral.depletion");
                                        data.setInteger("depletion" + k + kk, MMMineralMix);
                                        data.setString("kmm" + k + kk, "empty");
                                    }
                                    event.getActiveRecipe().setMaxParallelism(8);
                                })
                                .addFactoryStartHandler(event -> {
                                    var ctrl = event.getController();
                                    var data = ctrl.getCustomDataTag();
                                    data.getString("kmm" + k + kk);
                                    int x;
                                    int z;
                                    World world;
                                    if (isDimensional()) {
                                        var poss = data.getIntArray("pos");
                                        x = poss[0];
                                        z = poss[2];
                                        world = DimensionManager.getWorld(poss[3]);
                                    } else {
                                        x = ctrl.getPos().getX();
                                        z = ctrl.getPos().getZ();
                                        world = ctrl.getWorld();
                                    }
                                    var bxs = event.getFactoryRecipeThread().getActiveRecipe().getParallelism();
                                    var worldInfo = ExcavatorHandler.getMineralWorldInfo(
                                            world,
                                            (chunkCoord(x) + i),
                                            (chunkCoord(z) + ii)
                                    );
                                    var mineral = getUsableMix(worldInfo);
                                    if (mineral != null) {
                                        data.setString("kmm" + k + kk, mineral.name);
                                    }
                                    data.setInteger("sfsh", 6000);
                                    data.setInteger("bxs" + k + kk, bxs);
                                })
                                .addInput(dust).setChance(0.05f);
                        for (int j = 0; j < 3; j++) {
                            r1.addOutput(stone)
                                    .addItemModifier((ctrl, item) -> {
                                        if (isDimensional()) {
                                            var poss = ctrl.getController().getCustomDataTag().getIntArray("pos");
                                            var pos = new BlockPos(poss[0], poss[1], poss[2]);
                                            return getOreOutput(ctrl.getController(), pos, poss[3]);
                                        } else {
                                            return getOreOutput(ctrl.getController(), ctrl.getController().getPos(), ctrl.getIWorld().getDimension());
                                        }
                                    });
                        }

                        requireComputationPoint(r1, 3);
                        r1.addOutput(stone).addItemModifier((ctrl, item) -> getCcrystalOutput(ctrl.getController())).setChance(0.04f)
                                .setMaxThreads(1)
                                .setParallelized(true)
                                .setThreadName(threadName)
                                .build();
                        thread.addRecipe(recipeName);
                    }
                }
            }
        }
        machine.setInternalParallelism(getParallelism());
        HyperNetHelper.proxyMachineForHyperNet(getRegistryName());
        if (!isDimensional()) {
            switch (getType()) {
                case SINGLE -> machine.addMachineEventHandler(MachineStructureFormedEvent.class, event -> {
                    var ctrl = event.getController();
                    var data = ctrl.getCustomDataTag();
                    var x = ctrl.getPos().getX();
                    var z = ctrl.getPos().getZ();
                    var world = ctrl.getWorld();
                    var kmm = data.getString("kmm11");
                    var worldInfo = ExcavatorHandler.getMineralWorldInfo(
                            world,
                            chunkCoord(x),
                            chunkCoord(z)
                    );
                    var mineral = getUsableMix(worldInfo);
                    if (mineral != null) {
                        if (!kmm.equals(mineral.name)) {
                            data.setString("kmm11", mineral.name);
                        }
                    } else {
                        data.setString("kmm11", "empty");
                    }
                });
                case RANGE -> machine.addMachineEventHandler(MachineStructureFormedEvent.class, event -> {
                    var ctrl = event.getController();
                    var data = ctrl.getCustomDataTag();
                    var x = ctrl.getPos().getX();
                    var z = ctrl.getPos().getZ();
                    var world = ctrl.getWorld();
                    for (int i : tqsz) {
                        var k = i + 1;
                        for (int ii : tqsz) {
                            var kk = ii + 1;
                            var worldInfo = ExcavatorHandler.getMineralWorldInfo(
                                    world,
                                    chunkCoord(x) + i,
                                    chunkCoord(z) + ii
                            );
                            var mineral = getUsableMix(worldInfo);
                            if (mineral != null) {
                                data.setString("kmm" + k + kk, mineral.name);
                            } else {
                                data.setString("kmm" + k + kk, "empty");
                            }
                            if (!data.getString("kmm" + k + kk).equals("empty")) {
                                data.setInteger("depletion" + k + kk, worldInfo.depletion);
                            }
                        }
                    }
                });
            }
        }
        //旧版本的兼容，将旧的研究进度和组件nbt转为新的
        machine.addMachineEventHandler(MachineStructureUpdateEvent.class, event -> {
            var data = event.getController().getCustomDataTag();
            if (data.hasKey("yjjd")) {
                val yjjd = data.getByte("yjjd");
                data.setByte("research_progress", yjjd);
                data.removeTag("yjjd");
            }
            if (data.hasKey("zzsl")) {
                val zzsl = data.getByte("zzsl");
                data.setByte("components_amount", zzsl);
                data.removeTag("zzsl");
            }
        });
    }

    @SideOnly(Side.CLIENT)
    private void regGui(DynamicMachine machine) {
        switch (getType()) {
            case SINGLE -> machine.addMachineEventHandler(ControllerGUIRenderEvent.class, event -> {
                var ctrl = event.getController();
                var data = ctrl.getCustomDataTag();
                var research_progress = data.getByte("research_progress");
                var components_amount = data.getByte("components_amount");
                List<String> info = new ObjectArrayList<>();
                info.add(
                        I18n.format("top.drill.status") + "§6[" +
                                I18n.format("top.drill.research_progress") + research_progress + "|" +
                                I18n.format("top.drill.components_amount") + components_amount + "§6]"

                );
                if (data.hasKey("additional_component_raw_ore")) info.add(I18n.format("top.drill.components_raw_ore"));
                var kmm = data.getString("kmm11");
                var depletion = data.getInteger("depletion11");
                if (!kmm.isEmpty() && !kmm.equals("empty"))
                    info.add(I18n.format("novaeng.drill.mineral.name") + kmm);
                else if (kmm.isEmpty())
                    info.add(I18n.format("novaeng.drill.mineral.empyt.s"));
                if (!kmm.isEmpty() && !kmm.equals("empty"))
                    info.add(I18n.format("novaeng.drill.mineral.depletion.s") + (MMMineralMix - depletion));
                if (kmm.isEmpty()) info.add(I18n.format("novaeng.drill.mineral.empyt"));
                event.setExtraInfo(info.toArray(new String[0]));
            });
            case RANGE -> machine.addMachineEventHandler(ControllerGUIRenderEvent.class, event -> {
                var ctrl = event.getController();
                var data = ctrl.getCustomDataTag();
                var research_progress = data.getByte("research_progress");
                var components_amount = data.getByte("components_amount");
                List<String> info = new ObjectArrayList<>();
                info.add(
                        I18n.format("top.drill.status") + "§6[" +
                                I18n.format("top.drill.research_progress") + research_progress + "|" +
                                I18n.format("top.drill.components_amount") + components_amount + "§6]"

                );
                if (data.hasKey("additional_component_raw_ore")) info.add(I18n.format("top.drill.components_raw_ore"));
                for (int i : tqsz) {
                    var k = i + 1;
                    for (int ii : tqsz) {
                        var kk = ii + 1;
                        var kmm = data.getString("kmm" + k + kk);
                        var depletion = data.getInteger("depletion" + k + kk);
                        if (!kmm.isEmpty() && !kmm.equals("empty"))
                            info.add(
                                    I18n.format(
                                            "novaeng.drill.mineral.depletion.r",
                                            tqdzb.getInt((String.valueOf(k) + kk)),
                                            kmm
                                    ) + (MMMineralMix - depletion)
                            );
                        else if (kmm.isEmpty())
                            info.add(
                                    I18n.format("novaeng.drill.mineral.empyt.r",
                                            tqdzb.getInt((String.valueOf(k) + kk))
                                    )
                            );
                        if (kmm.equals("empty")) info.add(I18n.format("novaeng.drill.mineral.empyt"));
                    }
                }
                event.setExtraInfo(info.toArray(new String[0]));
            });
        }
    }

    private void regUpgrade(final DynamicMachine machine) {
        var upThreadName = "novaeng.drill.thread.up";
        var upThread = FactoryRecipeThread.createCoreThread(upThreadName);
        var name = getMachineName();
        if (isDimensional()) {
            RecipeBuilder.newBuilder("excavatorzb" + name, name, 10)
                    .addInput(itemUtils.getItem("contenttweaker:zbk", 0))
                    .setNBTChecker((ctrl, item) -> {
                        var data = ctrl.getController().getCustomDataTag();
                        if (!data.hasKey("binding")) {
                            return false;
                        }
                        var pos = IDataUtils.getIntArray(item.getTag(),"pos",null);
                        if (pos == null) {
                            return false;
                        }
                        data.setIntArray("poss", pos);
                        return true;
                    })
                    .addOutput(itemUtils.getItem("contenttweaker:zbk", 0))
                    .addPreCheckHandler(event -> {
                        var ctrl = event.getController();
                        if (ctrl.isWorking()) {
                            event.setFailed(translateToLocalFormatted("novaeng.machine.failed.work"));
                        }
                    })
                    .addFactoryStartHandler(event -> {
                        var ctrl = event.getController();
                        var data = ctrl.getCustomDataTag();
                        var poss = data.getIntArray("poss");
                        var world = DimensionManager.getWorld(poss[3]);
                        switch (getType()) {
                            case SINGLE -> {
                                var worldInfo = ExcavatorHandler.getMineralWorldInfo(
                                        world,
                                        chunkCoord(poss[0]),
                                        chunkCoord(poss[2])
                                );
                                var mineral = getUsableMix(worldInfo);
                                if (mineral != null) {
                                    data.setString("kmm11", mineral.name);
                                }
                                if (!data.getString("kmm11").isEmpty()) {
                                    data.setInteger("depletion11", worldInfo.depletion);
                                }
                            }
                            case RANGE -> {
                                for (int i : tqsz) {
                                    var k = i + 1;
                                    for (int ii : tqsz) {
                                        var kk = ii + 1;
                                        var worldInfo = ExcavatorHandler.getMineralWorldInfo(
                                                world,
                                                chunkCoord(poss[0]) + i,
                                                chunkCoord(poss[2]) + ii
                                        );
                                        var mineral = getUsableMix(worldInfo);
                                        if (mineral != null) {
                                            data.setString("kmm" + k + kk, mineral.name);
                                        }
                                        if (!data.getString("kmm" + k + kk).isEmpty()) {
                                            data.setInteger("depletion" + k + kk, worldInfo.depletion);
                                        }
                                    }
                                }
                            }
                        }
                        data.setIntArray("pos", poss);
                    })
                    .setParallelized(false)
                    .setThreadName(upThreadName)
                    .build();
            upThread.addRecipe("excavatorzb" + name);
        }
        for (int i = 0; i < 3; i++) {
            var fi = i;
            upThread.addRecipe("research_mineral_utilization_" + name + "_" + fi);
            upThread.addRecipe("additional_component_loading_" + name + "_" + fi);
            requireResearch(
                    RecipeBuilder.newBuilder("research_mineral_utilization_" + name + "_" + fi, name, 10)
                            .addPreCheckHandler(event -> {
                                var ctrl = event.getController();
                                var data = ctrl.getCustomDataTag();
                                data.getByte("research_progress");
                                data.getByte("components_amount");
                                var component = data.getBoolean("research_mineral_" + fi);
                                if (component) {
                                    event.setFailed("novaeng.machine.failed.work");
                                }
                            })
                            .addFactoryFinishHandler(event -> {
                                var ctrl = event.getController();
                                var data = ctrl.getCustomDataTag();
                                var research_progress = data.getByte("research_progress");

                                ctrl.addPermanentModifier("research" + fi, RecipeModifierBuilder.create("modularmachinery:energy", "input", (float) (1 + (0.2 * (fi + 1))), 1, false).build());
                                data.setBoolean("research_mineral_" + fi, true);
                                data.setByte("research_progress", (byte) (research_progress + 1));
                            })
                    , "research_mineral_utilization_" + fi)
                    .setParallelized(false)
                    .setThreadName(upThreadName)
                    .setLoadJEI(false)
                    .build();
            requireResearch(
                    RecipeBuilder.newBuilder("additional_component_loading_" + name + "_" + fi, name, 100, 1)
                            .addItemInput(itemUtils.getItem("contenttweaker:additional_component_" + fi, 0))
                            .addPreCheckHandler(event -> {
                                var ctrl = event.getController();
                                var data = ctrl.getCustomDataTag();
                                data.getByte("research_progress");
                                data.getByte("components_amount");
                                var component = data.getBoolean("additional_component_" + fi);
                                if (component) {
                                    event.setFailed("novaeng.machine.failed.work");
                                }
                            })
                            .addFactoryFinishHandler(event -> {
                                var ctrl = event.getController();
                                var data = ctrl.getCustomDataTag();
                                var components_amount = data.getByte("components_amount");

                                ctrl.addPermanentModifier("additional" + fi, RecipeModifierBuilder.create("modularmachinery:energy", "input", (float) (1 + (0.3 * (fi + 1))), 1, false).build());
                                ctrl.addPermanentModifier("additionalout", RecipeModifierBuilder.create("modularmachinery:item", "output", (float) (Math.pow(components_amount + 1, 3) * 2), 1, false).build());

                                data.setBoolean("additional_component_" + fi, true);
                                data.setByte("additional_component_" + fi, (byte) (components_amount + 1));
                            })
                    , "additional_component_loading_" + fi)
                    .setThreadName(upThreadName)
                    .setParallelized(false)
                    .setLoadJEI(false)
                    .build();
        }
        upThread.addRecipe("additional_component_loading_" + name + "_3");
        requireResearch(
                RecipeBuilder.newBuilder("additional_component_loading_" + name + "_3", name, 100, 1)
                        .addItemInput(itemUtils.getItem("contenttweaker:additional_component_3", 0))
                        .addPreCheckHandler(event -> {
                            var ctrl = event.getController();
                            var data = ctrl.getCustomDataTag();
                            data.getByte("research_progress");
                            data.getByte("components_amount");
                            var additional_component_3 = data.getBoolean("additional_component_3");
                            if (additional_component_3) {
                                event.setFailed("novaeng.machine.failed.work");
                            }
                        })
                        .addFactoryFinishHandler(event -> {
                            var ctrl = event.getController();
                            var data = ctrl.getCustomDataTag();
                            var components_amount = data.getByte("components_amount");
                            var research_progress = data.getByte("research_progress");
                            ctrl.addPermanentModifier("additional_ex", RecipeModifierBuilder.create("modularmachinery:energy", "input", 4, 1, false).build());
                            ctrl.addPermanentModifier("additionalout", RecipeModifierBuilder.create("modularmachinery:item", "output", (float) (Math.pow(components_amount + 1, 3) * 2), 1, false).build());
                            data.setBoolean("additional_component_3", true);
                            data.setByte("components_amount", (byte) (components_amount + 1));
                            data.setByte("research_progress", (byte) (research_progress + 1));
                        })
                        .setThreadName(upThreadName)
                        .setParallelized(false)
                        .setLoadJEI(false)
                , "additional_component_loading_ex")
                .build();
        upThread.addRecipe("additional_component_loading_" + name + "_raw_ore");
        requireResearch(
                RecipeBuilder.newBuilder("additional_component_loading_" + name + "_raw_ore", name, 100, 1)
                        .addItemInput(itemUtils.getItem("contenttweaker:additional_component_raw_ore", 0))
                        .addPreCheckHandler(event -> {
                            var ctrl = event.getController();
                            var data = ctrl.getCustomDataTag();
                            data.getByte("research_progress");
                            data.getByte("components_amount");
                            var additional_component_raw_ore = data.getBoolean("additional_component_raw_ore");
                            if (additional_component_raw_ore) {
                                event.setFailed("novaeng.machine.failed.work");
                            }
                        })
                        .addFactoryFinishHandler(event -> {
                            var ctrl = event.getController();
                            var data = ctrl.getCustomDataTag();
                            data.setBoolean("additional_component_raw_ore", true);
                            ctrl.addPermanentModifier("additional_raw_ore", RecipeModifierBuilder.create("modularmachinery:energy", "input", 2, 1, false).build());
                            ctrl.setCustomDataTag(data);
                        })
                , "additional_component_loading_raw_ore")
                .setThreadName(upThreadName)
                .setParallelized(false)
                .setLoadJEI(false)
                .build();
        machine.addCoreThread(upThread);
        machine.setMaxThreads(0);
    }

    @Override
    public void onTOPInfo(final ProbeMode probeMode,
                          final IProbeInfo probeInfo,
                          final EntityPlayer player,
                          final IProbeHitData ipData,
                          final TileMultiblockMachineController controller) {
        var data = controller.getCustomDataTag();
        var research_progress = data.getByte("research_progress");
        var components_amount = data.getByte("components_amount");
        MachineSpecial.newBox(probeInfo)
                .text("{*top.drill.status*}  ")
                .text("{*top.drill.research_progress*}" + research_progress + "  ")
                .text("{*top.drill.components_amount*}" + components_amount);
    }

    protected enum Type {
        SINGLE,
        RANGE
    }

    protected static int chunkCoord(int posValue) {
        return posValue >> 4;
    }

    private static IItemStack getOreOutput(TileMultiblockMachineController ctrl, BlockPos pos, int worldid) {
        return getOreOutput(ctrl, pos, worldid, 0, 0);
    }

    private static IItemStack getOreOutput(TileMultiblockMachineController ctrl, BlockPos pos, int worldid, int k, int kk) {
        var world = DimensionManager.getWorld(worldid);
        if (world == null) {
            return errorStone.mutable().copy();
        }
        var data = ctrl.getCustomDataTag();
        var research_progress = data.getByte("research_progress");
        var components_amount = data.getByte("components_amount");
        var sfsh = Math.max((data.hasKey("sfsh") ? data.getInteger("sfsh") : 10000) - (1000 * Math.pow(research_progress, 1.6)), 0);
        var bxs = data.getInteger("bxs" + (k + 1) + (kk + 1)) * (1 + Math.pow(components_amount, 2.5));
        var component_raw_ore = data.getByte("additional_component_raw_ore");
        var random = ctrl.getWorld().rand.nextInt(10000);
        var worldInfo = ExcavatorHandler.getMineralWorldInfo(
                world,
                (chunkCoord(pos.getX()) + k),
                (chunkCoord(pos.getZ()) + kk)
        );
        if (sfsh < 10000) {
            if (sfsh > random) {
                worldInfo.depletion += (int) bxs;
                data.setInteger("depletion" + (k + 1) + (kk + 1), worldInfo.depletion);
                ctrl.setCustomDataTag(data);
            }
        } else {
            var sl = Math.floor(1.0f * sfsh) / 10000;
            worldInfo.depletion += (int) (bxs * sl);
            if ((sfsh - (10000 * sl)) > random) {
                worldInfo.depletion += (int) bxs;
            }
            data.setInteger("depletion" + (k + 1) + (kk + 1), worldInfo.depletion);
            ctrl.setCustomDataTag(data);
        }
        var mineral = getUsableMix(worldInfo);
        if (mineral != null) {
            var iore = mineral.getRandomOre(world.rand);
            if (component_raw_ore == 1) {
                var rawore = OreHandler.getRawOre(iore);
                if (rawore == null) {
                    return OreHandler.getOre(iore);
                } else {
                    random = world.rand.nextInt(6);
                    return rawore.amount(Math.max(random, 1));
                }
            }
            return OreHandler.getOre(iore);
        } else {
            data.setString("kmm" + (k + 1) + (kk + 1), "empty");
            return stone;
        }
    }

    private static IItemStack getCcrystalOutput(TileMultiblockMachineController ctrl) {
        var data = ctrl.getCustomDataTag();
        var world = ctrl.getWorld();
        var research_progress = data.getByte("research_progress");
        if (research_progress > 0) {
            List<IItemStack> cjc = new ObjectArrayList<>();
            cjc.add(itemUtils.getItem("environmentaltech:erodium_crystal", 0));
            if (research_progress > 1) {
                cjc.add(itemUtils.getItem("environmentaltech:kyronite_crystal", 0));
                cjc.add(itemUtils.getItem("environmentaltech:pladium_crystal", 0));
                if (research_progress > 2) {
                    cjc.add(itemUtils.getItem("environmentaltech:ionite_crystal", 0));
                    if (research_progress > 3) {
                        cjc.add(itemUtils.getItem("environmentaltech:aethium_crystal", 0));
                    }
                }
            }
            var random = world.rand.nextInt(cjc.size() - 1);
            return cjc.get(random);
        } else {
            return stone;
        }
    }

    @Nullable
    private static ExcavatorHandler.MineralMix getUsableMix(ExcavatorHandler.MineralWorldInfo worldInfo){
        if (worldInfo.mineralOverride != null){
            return worldInfo.mineralOverride;
        } else {
            return worldInfo.mineral;
        }
    }
}