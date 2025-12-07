package github.kasuminova.novaeng.common.machine

import crafttweaker.CraftTweakerAPI
import crafttweaker.api.minecraft.CraftTweakerMC
import github.kasuminova.novaeng.common.crafttweaker.expansion.RecipePrimerHyperNet.requireResearch
import hellfirepvp.modularmachinery.ModularMachinery
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeModifierBuilder
import hellfirepvp.modularmachinery.common.machine.DynamicMachine
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier
import mustapelto.deepmoblearning.common.metadata.MetadataManager
import net.minecraft.util.ResourceLocation

//TODO:处理硬编码
object MaterialSequenceProcessing : MachineSpecial {

    private const val MACHINEID = "material_sequence_processing"
    val REGISTRY_NAME = ResourceLocation(ModularMachinery.MODID, MACHINEID)

    override fun preInit(machine: DynamicMachine?) {
        for (model in MetadataManager.getDataModelMetadataList()) {
            if (!model.isEnabled) continue
            val item = model.pristineMatter
            val loot = model.lootItems
            if (!loot.isEmpty()) {
                for (i in loot.indices) {
                    val item0 = loot[i]
                    val tag = StringBuilder()
                    val tagname = StringBuilder()
                    if (i < 3) {
                        tag.append("left").append(i + 1)
                        tagname.append("在左").append(i + 1).append("仓室执行此配方")
                    } else if (i < 6) {
                        tag.append("right").append(i - 2)
                        tagname.append("在右").append(i - 2).append("仓室执行此配方")
                    } else break
                    RecipeBuilder.newBuilder(MACHINEID + item.item.getRegistryName() + i, MACHINEID, 20, 1)
                        .addEnergyPerTickInput(204800)
                        .addInputs(CraftTweakerMC.getIItemStack(item)).setTag(tag.toString())
                        .addCatalystInput(
                            CraftTweakerAPI.itemUtils.getItem("contenttweaker:hxs", 0),
                            arrayOf(
                                "输入核心素催化物质重组,产物增加25%,每并行需要一个",
                                "并不能增加单次产出低于4的产物数量.."
                            ),
                            arrayOf<RecipeModifier?>(
                                RecipeModifierBuilder.create(
                                    "modularmachinery:item",
                                    "output",
                                    1.25f,
                                    1,
                                    false
                                ).build()
                            )
                        ).setChance(0.01f)
                        .addOutputs(CraftTweakerMC.getIItemStack(item0))
                        .requireResearch(
                            "pristine"
                        )
                        .addRecipeTooltip(tagname.toString(), "核心素可以在任意仓内")
                        .build()
                }
            }
        }
    }

    override fun getRegistryName(): ResourceLocation {
        return REGISTRY_NAME
    }
}