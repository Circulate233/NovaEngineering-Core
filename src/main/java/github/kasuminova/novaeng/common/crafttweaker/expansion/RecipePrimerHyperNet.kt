package github.kasuminova.novaeng.common.crafttweaker.expansion

import crafttweaker.annotations.ZenRegister
import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils
import github.kasuminova.novaeng.common.hypernet.old.NetNodeCache
import github.kasuminova.novaeng.common.hypernet.old.NetNodeImpl
import github.kasuminova.novaeng.common.hypernet.old.research.ResearchCognitionData
import github.kasuminova.novaeng.common.registry.RegistryHyperNet
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.client.resources.I18n
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fml.common.FMLCommonHandler
import stanhebben.zenscript.annotations.Optional
import stanhebben.zenscript.annotations.ZenExpansion
import stanhebben.zenscript.annotations.ZenMethod
import java.util.Arrays
import java.util.stream.Collectors

@ZenRegister
@ZenExpansion("mods.modularmachinery.RecipePrimer")
object RecipePrimerHyperNet {

    /**
     * 为一个配方添加算力要求。
     * 用法：`requireComputationPoint(1.0F);`
     */
    @JvmStatic
    fun RecipePrimer.requireComputationPoint(
        required: Float
    ): RecipePrimer {
        return this.requireComputationPoint(required, false)
    }

    @JvmStatic
    @ZenMethod
    fun RecipePrimer.requireComputationPoint(
        required: Float,
        @Optional triggerFailure: Boolean
    ): RecipePrimer {
        if (FMLCommonHandler.instance().side.isClient) {
            this.addRecipeTooltip(
                I18n.format(
                    "novaeng.hypernet.computation_point_required.tip",
                    NovaEngUtils.formatFLOPS(required.toDouble())
                ).intern()
            )
        }

        return this.addPreCheckHandler {
            val ctrl = it.getController()
            val node = NetNodeCache.getCache(ctrl, NetNodeImpl::class.java)
            node?.checkComputationPoint(it, required.toDouble())
        }.addStartHandler {
            val ctrl = it.getController()
            val node = NetNodeCache.getCache(ctrl, NetNodeImpl::class.java)
            node?.onRecipeStart(it, required.toDouble())
        }.addFactoryStartHandler {
            val ctrl = it.getController()
            val node = NetNodeCache.getCache(ctrl, NetNodeImpl::class.java)
            node?.onRecipeStart(it, required.toDouble())
        }.addPreTickHandler {
            val ctrl = it.getController()
            val node = NetNodeCache.getCache(ctrl, NetNodeImpl::class.java)
            node?.onRecipePreTick(it, required.toDouble(), triggerFailure)
        }.addFactoryPreTickHandler {
            val ctrl = it.getController()
            val node = NetNodeCache.getCache(ctrl, NetNodeImpl::class.java)
            node?.onRecipePreTick(it, required.toDouble(), triggerFailure)
        }.addFactoryFinishHandler {
            val ctrl = it.getController()
            val node = NetNodeCache.getCache(ctrl, NetNodeImpl::class.java)
            node?.onRecipeFinished(it.getRecipeThread())
        }.addFinishHandler {
            val ctrl = it.getController()
            val node = NetNodeCache.getCache(ctrl, NetNodeImpl::class.java)
            node?.onRecipeFinished(it.recipeThread)
        }
    }

    /**
     * 为一个配方添加研究认知要求。
     * 用法：`requireResearch("research_name_a", "research_name_b")`
     */
    @JvmStatic
    @ZenMethod
    fun RecipePrimer.requireResearch(
        vararg researchNames: String
    ): RecipePrimer {
        val list = ObjectArrayList<ResearchCognitionData>()
        researchNames.forEach {
            val data = RegistryHyperNet.getResearchCognitionData(it)
            if (data != null) {
                list.add(data)
            }
        }
        return this.requireResearch(*list.toTypedArray())
    }

    @JvmStatic
    @ZenMethod
    fun RecipePrimer.requireResearch(
        vararg researchRequired: ResearchCognitionData
    ): RecipePrimer {
        if (FMLCommonHandler.instance().side.isClient) {
            val researchTip = Arrays.stream(researchRequired)
                .map { it.translatedName }
                .collect(Collectors.joining("${TextFormatting.RESET}, "))
            this.addRecipeTooltip(I18n.format("novaeng.hypernet.research_required.tip", researchTip).intern())
        }

        return this.addPostCheckHandler {
            val cache = NetNodeCache.getCache(it.getController(), NetNodeImpl::class.java)
            cache?.checkResearch(it, *researchRequired)
        }
    }
}