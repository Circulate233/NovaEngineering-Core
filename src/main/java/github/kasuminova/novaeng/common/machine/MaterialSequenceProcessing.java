package github.kasuminova.novaeng.common.machine;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.api.minecraft.CraftTweakerMC;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeModifierBuilder;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import mustapelto.deepmoblearning.common.metadata.MetadataManager;
import net.minecraft.util.ResourceLocation;

import static github.kasuminova.novaeng.common.crafttweaker.expansion.RecipePrimerHyperNet.requireResearch;

public class MaterialSequenceProcessing implements MachineSpecial {
    private static final String MachineID = "material_sequence_processing";
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, MachineID);
    public static final MaterialSequenceProcessing INSTANCE = new MaterialSequenceProcessing();

    @Override
    public void init(final DynamicMachine machine) {
        MetadataManager.getDataModelMetadataList().forEach(model -> {
            var item = model.getPristineMatter();
            var loot = model.getLootItems();
            if (!loot.isEmpty()){
                for (int i = 0; i < loot.size(); i++) {
                    var item0 = loot.get(i);
                    var tag = new StringBuilder();
                    var tagname = new StringBuilder();
                    if (i < 3){
                        tag.append("left").append(i + 1);
                        tagname.append("在左").append(i + 1).append("仓室执行此配方");
                    } else if (i < 6) {
                        tag.append("right").append(i - 2);
                        tagname.append("在右").append(i - 2).append("仓室执行此配方");
                    } else continue;
                    var pecipe = RecipeBuilder.newBuilder(MachineID + item.getItem().getRegistryName() + i, MachineID, 20,1)
                            .addEnergyPerTickInput(204800)
                            .addInputs(CraftTweakerMC.getIItemStack(item)).setTag(tag.toString())
                            .addCatalystInput(
                                CraftTweakerAPI.itemUtils.getItem("contenttweaker:hxs",0),
                                new String[]{"输入核心素催化物质重组,产物增加25%,每并行需要一个", "并不能增加单次产出低于4的产物数量.."},
                                new RecipeModifier[]{RecipeModifierBuilder.create("modularmachinery:item", "output", 1.25f, 1, false).build()}
                            ).setChance(0.01f)
                            .addOutputs(CraftTweakerMC.getIItemStack(item0));
                    requireResearch(pecipe,"pristine")
                            .addRecipeTooltip(tagname.toString(),"核心素可以在任意仓内")
                            .build();
                }
            }
        });
    }

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }

}
