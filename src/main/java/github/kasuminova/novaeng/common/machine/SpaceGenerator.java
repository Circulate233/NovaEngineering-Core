package github.kasuminova.novaeng.common.machine;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.api.item.IIngredient;
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
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import morph.avaritia.recipe.AvaritiaRecipeManager;
import net.minecraft.util.ResourceLocation;

import java.util.List;

public final class SpaceGenerator implements MachineSpecial {
    public static final SpaceGenerator INSTANCE = new SpaceGenerator();
    private static final String MACHINEID = "space_generator";
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, MACHINEID);

    private SpaceGenerator() {
    }

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }

    @Override
    public void preInit(final DynamicMachine machine) {
        final List<String> ores = new ObjectArrayList<>();
        final var singularities = new Object2LongOpenHashMap<String>();
        for (final var entry : CraftTweakerAPI.oreDict.getEntries()) {
            final String name = entry.getName();
            if (name.startsWith("ore") && !entry.isEmpty()) {
                final String ore = name.substring(3);
                if (ore.equals("Aluminum")) {
                    continue;
                }
                final var ingot = CraftTweakerAPI.oreDict.get("ingot" + ore);
                final var gem = CraftTweakerAPI.oreDict.get("gem" + ore);
                final var dust = CraftTweakerAPI.oreDict.get("dust" + ore);
                final IOreDictEntry product = !ingot.isEmpty() ? ingot : !gem.isEmpty() ? gem : !dust.isEmpty() ? dust : null;
                if (product != null) {
                    ores.add("Ore" + ore);
                    final var singularity = CraftTweakerAPI.oreDict.get("singularity" + ore);
                    this.inputRecipe("space_Ore" + ore, entry, product, singularity, ore, 1,
                        "novaeng.space_generator.recipe.ore.tooltips");
                    final var raw = CraftTweakerAPI.oreDict.get("rawOre" + ore);
                    if (!raw.isEmpty()) {
                        this.inputRecipe("space_Ore1" + ore, raw.amount(3), product, singularity, ore, 2,
                            "novaeng.space_generator.recipe.raw_ore.tooltips");
                    }
                    final var rawGem = CraftTweakerAPI.oreDict.get("rawOreGem" + ore);
                    if (!rawGem.isEmpty()) {
                        this.inputRecipe("space_Ore1" + ore, rawGem, product, singularity, ore, 1,
                            "novaeng.space_generator.recipe.gem.tooltips");
                    }
                }
            }
            if (name.startsWith("singularity") && !name.equals("singularity")) {
                final String type = name.substring(11);
                final var item = OreHandler.OreDictHelper.getPriorityItemFromOreDict(name);
                if (item.isEmpty()) {
                    continue;
                }
                final var recipe = AvaritiaRecipeManager.getCompressorRecipeFromResult(item);
                if (recipe == null) {
                    continue;
                }
                this.singularityOperation(CraftTweakerMC.getIItemStack(item), type);
                singularities.put(type, recipe.getCost() * (type.equals("Quartz") ? 4L : 9L));
            }
        }
        this.oreProcessing(ores, singularities);
        machine.addCoreThread(FactoryRecipeThread.createCoreThread("novaeng.space_generator.thread.core")
                                                 .addRecipe("hxzk1").addRecipe("hxzk2").addRecipe("hxzk3").addRecipe("hxzk4"));
        machine.addCoreThread(FactoryRecipeThread.createCoreThread("novaeng.space_generator.thread.energy").addRecipe("space_energy1").addRecipe("space_energy2"));
        machine.addCoreThread(FactoryRecipeThread.createCoreThread("novaeng.space_generator.thread.fluid").addRecipe("space_fluid"));
        machine.setInternalParallelism(2000000000);
        machine.setMaxThreads(0);
        for (int i = 0; i < 10; i++) {
            machine.addCoreThread(FactoryRecipeThread.createCoreThread("novaeng.space_generator.thread.item.input." + i));
        }
    }

    private void inputRecipe(final String name, final IIngredient input, final IOreDictEntry product,
                             final IOreDictEntry singularity, final String ore, final int multiplier, final String tooltip) {
        final var recipe = RecipeBuilder.newBuilder(name, MACHINEID, 20, 2).addInput(input)
                                        .addPreCheckHandler(event -> {
                                            if (event.getController().getCustomDataTag().getByte("hxzt") != 1) {
                                                event.setFailed("novaeng.space_generator.failed.item.input");
                                            }
                                        }).addFactoryFinishHandler(event -> {
                final var data = event.getController().getCustomDataTag();
                final int parallelism = event.getFactoryRecipeThread().getActiveRecipe().getParallelism();
                final long count = multiplier == 1 ? parallelism : parallelism * 2L;
                data.setLong("Ore" + ore, data.getLong("Ore" + ore) + count);
                data.setLong("kwzl", data.getLong("kwzl") + count);
            }).setMaxThreads(1).setParallelized(true).addRecipeTooltip(StringUtils.getTexts(tooltip));
        if (!singularity.isEmpty()) {
            RecipePrimerEx.setLore(recipe.addOutput(singularity.getFirstItem()).setChance(0), "novaeng.space_generator.recipe.ore.output.tooltips");
        }
        RecipePrimerEx.setLore(recipe.addOutput(CraftTweakerMC.getIItemStack(OreHandler.OreDictHelper.getPriorityItemFromOreDict(product.getName())))
                                     .setChance(0), "novaeng.space_generator.recipe.ore.output.tooltips").build();
    }

    private void oreProcessing(final List<String> ores, final Object2LongMap<String> singularities) {
        var recipe = RecipeBuilder.newBuilder("hxzk3", MACHINEID, 1, 99999).addPreCheckHandler(event -> {
            if (event.getController().getCustomDataTag().getByte("hxzt") != 3) {
                event.setFailed("novaeng.space_generator.failed.no_output");
            }
        });
        for (final String oreName : ores) {
            final String ore = oreName.substring(3);
            final var singularity = CraftTweakerAPI.oreDict.get("singularity" + ore);
            if (!singularity.isEmpty()) {
                recipe.addOutput(singularity.getFirstItem()).addItemModifier((controller, item) ->
                    this.output(oreName, controller, item, singularities.getLong(ore)));
            }
            final var ingot = CraftTweakerAPI.oreDict.get("ingot" + ore);
            final var gem = CraftTweakerAPI.oreDict.get("gem" + ore);
            final var dust = CraftTweakerAPI.oreDict.get("dust" + ore);
            final IOreDictEntry product = !ingot.isEmpty() ? ingot : !gem.isEmpty() ? gem : !dust.isEmpty() ? dust : null;
            if (product != null) {
                recipe = recipe.addOutput(CraftTweakerMC.getIItemStack(OreHandler.OreDictHelper.getPriorityItemFromOreDict(product.getName())))
                               .addItemModifier((controller, item) -> this.output(oreName, controller, item, 0));
            }
        }
        recipe.setParallelized(false).addFactoryFinishHandler(event -> event.getController().getCustomDataTag().setByte("hxzt", (byte) 0))
              .setLoadJEI(false).setThreadName("novaeng.space_generator.thread.core").build();
    }

    private IItemStack output(final String name, final IMachineController controller, final IItemStack item, final long cost) {
        final var data = controller.getController().getCustomDataTag();
        final int power = switch (name) {
            case "OreLapis" -> 5;
            case "OreRedstone" -> 6;
            case "OreCertusQuartz", "OreChargedCertusQuartz" -> 2;
            default -> 1;
        };
        final double amount = data.getDouble(name);
        final float first = data.getFloat("cq1"), second = data.getFloat("cq2");
        if (first == 0 || second == 0) {
            data.setLong(name, 0);
            return item.amount(0);
        }
        long added = 0;
        if (amount != 0) {
            final double factor = 4.0F * first * second * power;
            if (cost != 0) {
                final double maximum = cost / factor;
                if (amount > maximum) {
                    final long count = (long) Math.floor(amount / maximum);
                    added += count;
                    data.setLong(name, (long) (amount - cost * count / factor));
                }
            } else {
                added += (long) (amount * factor);
                data.setLong(name, 0);
            }
        }
        return item.amount(added >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) added);
    }

    private void singularityOperation(final IItemStack singularity, final String name) {
        final int cost = AvaritiaRecipeManager.getCompressorRecipeFromResult(CraftTweakerMC.getItemStack(singularity)).getCost();
        final long discounted = Math.round(0.7 * cost);
        final int factor = name.equals("Quartz") ? 4 : 9;
        final var ingot = CraftTweakerAPI.oreDict.get("ingot" + name);
        final var nugget = CraftTweakerAPI.oreDict.get("nugget" + name);
        final var block = CraftTweakerAPI.oreDict.get("block" + name);
        final var gem = CraftTweakerAPI.oreDict.get("gem" + name);
        final var dust = CraftTweakerAPI.oreDict.get("dust" + name);
        final var product = !ingot.isEmpty() ? ingot : !gem.isEmpty() ? gem : dust;
        RecipeBuilder.newBuilder("space_singularity1" + name, MACHINEID, 20, 3).addEnergyPerTickInput(100000000)
                     .addIngredientArrayInput(IngredientArrayBuilder.newBuilder().addIngredients(block.amount(cost), product.amount(factor * cost), nugget.amount(9 * factor * cost)))
                     .addPreCheckHandler(event -> {
                         if (event.getController().getCustomDataTag().getInteger("hxzt") != 2) {
                             event.setFailed("novaeng.space_generator.failed.singularity");
                         }
                     })
                     .addOutput(singularity).setMaxThreads(1).setParallelized(true)
                     .addRecipeTooltip("novaeng.space_generator.tooltip0", "novaeng.space_generator.tooltip1").build();
        RecipeBuilder.newBuilder("space_singularity2" + name, MACHINEID, 20, 3).addEnergyPerTickInput(100000000).addInput(singularity)
                     .addPreCheckHandler(event -> {
                         if (event.getController().getCustomDataTag().getInteger("hxzt") != 2) {
                             event.setFailed("novaeng.space_generator.failed.singularity");
                         }
                     })
                     .addOutputs(CraftTweakerMC.getIItemStack(OreHandler.OreDictHelper.getPriorityItemFromOreDict(product.getName())).amount((int) (factor * discounted)))
                     .setMaxThreads(1).setParallelized(true).addRecipeTooltip("novaeng.space_generator.tooltip0", "novaeng.space_generator.tooltip1").build();
    }
}
