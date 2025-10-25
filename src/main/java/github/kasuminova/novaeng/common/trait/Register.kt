package github.kasuminova.novaeng.common.trait

import github.kasuminova.novaeng.common.enchantment.MagicBreaking
import github.kasuminova.novaeng.common.item.ItemBasic
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import slimeknights.tconstruct.library.modifiers.Modifier
import slimeknights.tconstruct.tools.AbstractToolPulse

object Register : AbstractToolPulse() {

    var modifierTraitsF: MutableList<Modifier> = ObjectArrayList()
    var modifierTraitsT: MutableList<Modifier> = ObjectArrayList()

    fun registerModifiers() {
        val traitMagicBreaking = registerModifier(TraitMagicBreaking())
        traitMagicBreaking.addItem(ItemBasic.getItem(MagicBreaking.MAGICBREAKING.id + "_stone"), 1, 1)

        modifierTraitsF.add(traitMagicBreaking)
    }
}