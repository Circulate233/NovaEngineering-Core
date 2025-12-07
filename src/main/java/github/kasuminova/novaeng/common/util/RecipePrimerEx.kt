@file:Suppress("DEPRECATION")

package github.kasuminova.novaeng.common.util

import crafttweaker.annotations.ZenRegister
import crafttweaker.api.minecraft.CraftTweakerMC.getIData
import github.kasuminova.novaeng.mixin.util.NovaRAB
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer
import hellfirepvp.modularmachinery.common.machine.IOType
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.util.text.translation.I18n.translateToLocalFormatted
import stanhebben.zenscript.annotations.ZenExpansion
import stanhebben.zenscript.annotations.ZenMethod

@ZenRegister
@ZenExpansion("mods.modularmachinery.RecipePrimer")
object RecipePrimerEx {

    @ZenMethod
    @JvmStatic
    fun RecipePrimer.setItemTags(tagName: String, isInput: Boolean): RecipePrimer {
        if (this is NovaRAB) {
            if (isInput) {
                this.`n$setInTags`(tagName)
            } else {
                this.`n$setOutTags`(tagName)
            }
        }

        for (component in this.components) {
            if (component is RequirementItem) {
                when (component.getActionType()) {
                    IOType.OUTPUT -> {
                        if (isInput) continue
                    }

                    IOType.INPUT -> {
                        if (!isInput) continue
                    }
                }
                val tag = ComponentSelectorTag(tagName)
                component.setTag(tag)
            }
        }
        return this
    }

    @ZenMethod
    @JvmStatic
    fun RecipePrimer.setMultipleParallelized(multipe: Int): RecipePrimer {
        this.addPreCheckHandler {
            it.activeRecipe.setMaxParallelism((it.activeRecipe.maxParallelism * multipe))
        }
        return this
    }

    @JvmStatic
    @ZenMethod
    fun RecipePrimer.setLore(vararg key: String): RecipePrimer {
        val nbt = NBTTagCompound()
        val nbt1 = NBTTagCompound()
        val list = NBTTagList()
        for (s in key) {
            list.appendTag(NBTTagString(translateToLocalFormatted(s)))
        }
        nbt1.setTag("Lore", list)
        nbt.setTag("display", nbt1)
        this.setPreViewNBT(getIData(nbt))
        return this
    }

}
