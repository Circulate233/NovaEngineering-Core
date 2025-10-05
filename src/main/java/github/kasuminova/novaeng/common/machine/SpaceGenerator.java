package github.kasuminova.novaeng.common.machine;

import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.oredict.IOreDictEntry;
import github.kasuminova.mmce.common.helper.IMachineController;
import github.kasuminova.novaeng.common.handler.OreHandler;
import github.kasuminova.novaeng.common.util.RecipePrimerEx;
import github.kasuminova.novaeng.common.util.StringUtils;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.IngredientArrayBuilder;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import morph.avaritia.recipe.AvaritiaRecipeManager;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static crafttweaker.CraftTweakerAPI.oreDict;

public class SpaceGenerator implements MachineSpecial {
    private static final String MachineID = "space_generator";
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, MachineID);
    public static final SpaceGenerator INSTANCE = new SpaceGenerator();
    private static final String oreOD = "ore";
    private static final String singularityOD = "singularity";

    @Override
    public void preInit(final DynamicMachine machine) {
        final List<String> orenames = new ArrayList<>();
        final Map<String, Long> singularitys = new HashMap<>();

        for (IOreDictEntry ench : oreDict.getEntries()) {
            var odName = ench.getName();
            if (odName.startsWith(oreOD)) {
                if (ench.isEmpty())continue;
                var ore = odName.substring(oreOD.length());
                if (ore.equals("Aluminum")) continue;
                var ingot = oreDict.get("ingot" + ore);
                var gem = oreDict.get("gem" + ore);
                var dust = oreDict.get("dust" + ore);
                var rawOre = oreDict.get("rawOre" + ore);
                var rawOreGem = oreDict.get("rawOreGem" + ore);
                var singularity = oreDict.get(singularityOD + ore);
                IOreDictEntry ores = null;
                if (!ingot.isEmpty()) {
                    ores = ingot;
                } else if (!gem.isEmpty()) {
                    ores = gem;
                } else if (!dust.isEmpty()) {
                    ores = dust;
                }
                if (ores != null) {
                    orenames.add("Ore" + ore);
                    var rec0 = RecipeBuilder.newBuilder("space_Ore" + ore, "space_generator", 20, 2)
                            .addInput(ench)
                            .addPreCheckHandler(event -> {
                                var ctrl = event.getController();
                                var data = ctrl.getCustomDataTag();
                                var hxzt = data.getByte("hxzt");
                                if (hxzt != 1) {
                                    event.setFailed("novaeng.space_generator.failed.item.input");
                                }
                            })
                            .addFactoryFinishHandler(event -> {
                                var ctrl = event.getController();
                                var data = ctrl.getCustomDataTag();
                                var Ore1 = data.getLong("Ore" + ore);
                                var thread = event.getFactoryRecipeThread();
                                var bx = thread.getActiveRecipe().getParallelism();

                                data.setLong("Ore" + ore, Ore1 + bx);
                                data.setLong("kwzl",data.getLong("kwzl") + bx);
                            })
                            .setMaxThreads(1)
                            .setParallelized(true)
                            .addRecipeTooltip(StringUtils.getTexts("novaeng.space_generator.recipe.ore.tooltips"));
                    if (!singularity.isEmpty())
                        RecipePrimerEx.setLore(rec0.addOutput(singularity.getFirstItem()).setChance(0), "novaeng.space_generator.recipe.ore.output.tooltips");
                    RecipePrimerEx.setLore(rec0.addOutput(CraftTweakerMC.getIItemStack(OreHandler.OreDictHelper.getPriorityItemFromOreDict(ores.getName()))).setChance(0), "novaeng.space_generator.recipe.ore.output.tooltips").build();
                    if (!rawOre.isEmpty()) {
                        var rec1 = RecipeBuilder.newBuilder("space_Ore1" + ore, "space_generator", 20, 2)
                                .addInput(rawOre.amount(3))
                                .addPreCheckHandler(event -> {
                                    var ctrl = event.getController();
                                    var data = ctrl.getCustomDataTag();
                                    var hxzt = data.getByte("hxzt");
                                    if (hxzt != 1) {
                                        event.setFailed("novaeng.space_generator.failed.item.input");
                                    }
                                })
                                .addFactoryFinishHandler(event -> {
                                    var ctrl = event.getController();
                                    var data = ctrl.getCustomDataTag();
                                    var Ore1 = data.getLong("Ore" + ore);
                                    var thread = event.getFactoryRecipeThread();
                                    var bx = thread.getActiveRecipe().getParallelism();

                                    data.setLong("Ore" + ore, Ore1 + (bx * 2L));
                                    data.setLong("kwzl",data.getLong("kwzl") +(bx * 2L));
                                })
                                .setMaxThreads(1)
                                .setParallelized(true)
                                .addRecipeTooltip(StringUtils.getTexts("novaeng.space_generator.recipe.raw_ore.tooltips"));
                        if (!singularity.isEmpty())
                            RecipePrimerEx.setLore(rec1.addOutput(singularity.getFirstItem()).setChance(0), "novaeng.space_generator.recipe.ore.output.tooltips");
                        RecipePrimerEx.setLore(rec1.addOutput(CraftTweakerMC.getIItemStack(OreHandler.OreDictHelper.getPriorityItemFromOreDict(ores.getName()))).setChance(0), "novaeng.space_generator.recipe.ore.output.tooltips").build();
                    }
                    if (!rawOreGem.isEmpty()) {
                        var rec1 = RecipeBuilder.newBuilder("space_Ore1" + ore, "space_generator", 20, 2)
                                .addInput(rawOreGem)
                                .addPreCheckHandler(event -> {
                                    var ctrl = event.getController();
                                    var data = ctrl.getCustomDataTag();
                                    var hxzt = data.getByte("hxzt");
                                    if (hxzt != 1) {
                                        event.setFailed("novaeng.space_generator.failed.item.input");
                                    }
                                })
                                .addFactoryFinishHandler(event -> {
                                    var ctrl = event.getController();
                                    var data = ctrl.getCustomDataTag();
                                    var Ore1 = data.getLong("Ore" + ore);
                                    var thread = event.getFactoryRecipeThread();
                                    var bx = thread.getActiveRecipe().getParallelism();

                                    data.setLong("Ore" + ore, Ore1 + bx);
                                    data.setLong("kwzl",data.getLong("kwzl") + bx);
                                })
                                .setMaxThreads(1)
                                .setParallelized(true)
                                .addRecipeTooltip(StringUtils.getTexts("novaeng.space_generator.recipe.gem.tooltips"));
                        if (!singularity.isEmpty())
                            RecipePrimerEx.setLore(rec1.addOutput(singularity.getFirstItem()).setChance(0), "novaeng.space_generator.recipe.ore.output.tooltips");
                        RecipePrimerEx.setLore(rec1.addOutput(CraftTweakerMC.getIItemStack(OreHandler.OreDictHelper.getPriorityItemFromOreDict(ores.getName()))).setChance(0), "novaeng.space_generator.recipe.ore.output.tooltips").build();
                    }
                }
            }
            if (odName.startsWith(singularityOD) && !odName.equals(singularityOD)) {
                var singularityname = odName.substring(singularityOD.length());
                var s = OreHandler.OreDictHelper.getPriorityItemFromOreDict(odName);
                if (s.isEmpty())continue;
                var r = AvaritiaRecipeManager.getCompressorRecipeFromResult(s);
                if (r == null)continue;

                singularityOperation(CraftTweakerMC.getIItemStack(s),singularityname);

                var count = r.getCost();
                if (!singularityname.equals("Quartz")) {
                    singularitys.put(singularityname, count * 9L);
                } else {
                    singularitys.put(singularityname, count * 4L);
                }
            }
        }

        oreProcessing(orenames,singularitys);

        machine.addCoreThread(FactoryRecipeThread.createCoreThread("novaeng.space_generator.thread.core").addRecipe("hxzk1").addRecipe("hxzk2").addRecipe("hxzk3").addRecipe("hxzk4"));
        machine.addCoreThread(FactoryRecipeThread.createCoreThread("novaeng.space_generator.thread.energy").addRecipe("space_energy1").addRecipe("space_energy2"));
        machine.addCoreThread(FactoryRecipeThread.createCoreThread("novaeng.space_generator.thread.fluid").addRecipe("space_fluid"));

        machine.setInternalParallelism(2000000000);
        machine.setMaxThreads(0);
        for(var i = 0;i < 10;i++){
            machine.addCoreThread(FactoryRecipeThread.createCoreThread("novaeng.space_generator.thread.item.input." + i));
        }
    }

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }

    private static void oreProcessing(List<String> orenames,Map<String, Long> singularitys){
        var rec = RecipeBuilder.newBuilder("hxzk3", "space_generator", 1, 99999)
                .addPreCheckHandler(event -> {
                    var ctrl = event.getController();
                    var data = ctrl.getCustomDataTag();
                    var hxzt = data.getByte("hxzt");
                    if (hxzt != 3) {
                        event.setFailed("novaeng.space_generator.failed.no_output");
                    }
                });

        for (String orename : orenames) {
            var singularityname = orename.substring(oreOD.length());
            var singularity = oreDict.get(singularityOD + singularityname);
            if (!singularity.isEmpty()) {
                rec.addOutput(singularity.getFirstItem()).addItemModifier((ctrl, item) -> output(orename, ctrl, item, singularitys.get(singularityname)));
            }

            var Ore = orename.substring(oreOD.length());
            var ingot = oreDict.get("ingot" + Ore);
            var gem = oreDict.get("gem" + Ore);
            var dust = oreDict.get("dust" + Ore);
            IOreDictEntry ores = null;
            if (!ingot.isEmpty()) {
                ores = ingot;
            } else if (!gem.isEmpty()) {
                ores = gem;
            } else if (!dust.isEmpty()) {
                ores = dust;
            }
            if (ores != null) {
                rec = rec.addOutput(CraftTweakerMC.getIItemStack(OreHandler.OreDictHelper.getPriorityItemFromOreDict(ores.getName()))).addItemModifier((ctrl, oldItem) -> output(orename, ctrl, oldItem,0));
            }
        }
        rec.setParallelized(false).addFactoryFinishHandler(event -> {
                    var ctrl = event.getController();
                    var data = ctrl.getCustomDataTag();
                    data.setByte("hxzt", (byte) 0);
                })
                .setLoadJEI(false)
                .setThreadName("novaeng.space_generator.thread.core")
                .build();
    }

    private static IItemStack output(String i, IMachineController ctrl, IItemStack oldItem, long qdsl) {
        var data = ctrl.getController().getCustomDataTag();
        int power = switch (i) {
            case "OreLapis" -> 5;
            case "OreRedstone" -> 6;
            case "OreCertusQuartz", "OreChargedCertusQuartz" -> 2;
            default -> 1;
        };
        double oreAmount = data.getDouble(i);
        float efficiencyFactor1 = data.getFloat("cq1");
        float efficiencyFactor2 = data.getFloat("cq2");

        if (efficiencyFactor1 == 0.0f || efficiencyFactor2 == 0.0f) {
            data.setLong(i, 0);
            return oldItem.amount(0);
        }

        long stackSizeChange = 0;

        if (oreAmount != 0) {
            double totalFactor = 4.0f * efficiencyFactor1 * efficiencyFactor2 * power;

            if (qdsl != 0) {
                double maxProcessableByQdsl = qdsl / totalFactor;

                if (oreAmount > maxProcessableByQdsl) {
                    double requiredRatio = oreAmount / maxProcessableByQdsl;
                    long addedCount = (long) Math.floor(requiredRatio);
                    stackSizeChange += addedCount;

                    double remainingOre = oreAmount - (qdsl * addedCount) / totalFactor;
                    data.setLong(i, (long) remainingOre);
                }
            } else {
                double addedCount = oreAmount * totalFactor;
                stackSizeChange += (long) addedCount;
                data.setLong(i, 0);
            }
        }

        return oldItem.amount(stackSizeChange >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) stackSizeChange);
    }

    private static void singularityOperation(IItemStack singularity,String singularityname) {
        var arg0 = AvaritiaRecipeManager.getCompressorRecipeFromResult(CraftTweakerMC.getItemStack(singularity)).getCost();
        int arg1;
        final var arg2 = Math.round(0.7 * arg0);
        if (singularityname.equals("Quartz")) {
            arg1 = 4;
        } else {
            arg1 = 9;
        }
        var ingot = oreDict.get("ingot" + singularityname);
        var nugget = oreDict.get("nugget" + singularityname);
        var block = oreDict.get("block" + singularityname);
        var gem = oreDict.get("gem" + singularityname);
        var dust = oreDict.get("dust" + singularityname);
        final IOreDictEntry ores;
        if (!ingot.isEmpty()) {
            ores = ingot;
        } else if (!gem.isEmpty()) {
            ores = gem;
        } else {
            ores = dust;
        }
        RecipeBuilder.newBuilder("space_singularity1" + singularityname, "space_generator", 20, 3)
                .addEnergyPerTickInput(100000000)
                .addIngredientArrayInput(IngredientArrayBuilder.newBuilder().addIngredients(
                        block.amount(arg0),
                        ores.amount(arg1 * arg0),
                        nugget.amount(9 * arg1 * arg0)
                ))
                .addPreCheckHandler(event -> {
                    var ctrl = event.getController();
                    var data = ctrl.getCustomDataTag();
                    var hxzt = data.getInteger("hxzt");
                    if (hxzt != 2) {
                        event.setFailed("novaeng.space_generator.failed.singularity");
                    }
                })
                .addOutput(singularity)
                .setMaxThreads(1)
                .setParallelized(true)
                .addRecipeTooltip(
                        "novaeng.space_generator.tooltip0",
                        "novaeng.space_generator.tooltip1"
                )
                .build();
        RecipeBuilder.newBuilder("space_singularity2" + singularityname, "space_generator", 20, 3)
                .addEnergyPerTickInput(100000000)
                .addInput(singularity)
                .addPreCheckHandler(event -> {
                    var ctrl = event.getController();
                    var data = ctrl.getCustomDataTag();
                    var hxzt = data.getInteger("hxzt");
                    if (hxzt != 2) {
                        event.setFailed("novaeng.space_generator.failed.singularity");
                    }
                })
                .addOutputs(CraftTweakerMC.getIItemStack(OreHandler.OreDictHelper.getPriorityItemFromOreDict(ores.getName())).amount((int) (arg1 * arg2)))
                .setMaxThreads(1)
                .setParallelized(true)
                .addRecipeTooltip(
                        "novaeng.space_generator.tooltip0",
                        "novaeng.space_generator.tooltip1"
                )
                .build();
    }
}
