package github.kasuminova.novaeng.common.machine;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.api.minecraft.CraftTweakerMC;
import github.kasuminova.novaeng.common.crafttweaker.expansion.RecipePrimerHyperNet;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeModifierBuilder;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import mustapelto.deepmoblearning.common.metadata.MetadataManager;
import net.minecraft.util.ResourceLocation;

public final class MaterialSequenceProcessing implements MachineSpecial {

    public static final MaterialSequenceProcessing INSTANCE = new MaterialSequenceProcessing();
    private static final String MACHINEID = "material_sequence_processing";
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, MACHINEID);

    private MaterialSequenceProcessing() {
    }

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }

    @Override
    public void preInit(final DynamicMachine machine) {
        for (final var model : MetadataManager.getDataModelMetadataList()) {
            if (!model.isEnabled()) {
                continue;
            }
            final var item = model.getPristineMatter();
            final var loot = model.getLootItems();
            for (int i = 0; i < loot.size() && i < 6; i++) {
                final String tag = i < 3 ? "left" + (i + 1) : "right" + (i - 2);
                final String description = i < 3 ? "在左" + (i + 1) + "仓室执行此配方" : "在右" + (i - 2) + "仓室执行此配方";
                final var recipe = RecipeBuilder.newBuilder(MACHINEID + item.getItem().getRegistryName() + i, MACHINEID, 20, 1)
                                                .addEnergyPerTickInput(204800).addInputs(CraftTweakerMC.getIItemStack(item)).setTag(tag)
                                                .addCatalystInput(CraftTweakerAPI.itemUtils.getItem("contenttweaker:hxs", 0),
                                                    new String[]{"输入核心素催化物质重组,产物增加25%,每并行需要一个", "并不能增加单次产出低于4的产物数量.."},
                                                    new RecipeModifier[]{RecipeModifierBuilder.create("modularmachinery:item", "output", 1.25F, 1, false).build()})
                                                .setChance(0.01F).addOutputs(CraftTweakerMC.getIItemStack(loot.get(i)));
                RecipePrimerHyperNet.requireResearch(recipe, "pristine")
                                    .addRecipeTooltip(description, "核心素可以在任意仓内").build();
            }
        }
    }
}
