package github.kasuminova.novaeng.common.trait;

import github.kasuminova.novaeng.common.enchantment.MagicBreaking;
import github.kasuminova.novaeng.common.item.ItemBasic;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.tools.AbstractToolPulse;

import java.util.List;

public class Register extends AbstractToolPulse {

    public static Register TRAITREGISTER = new Register();
    public List<Modifier> modifierTraitsF = new ObjectArrayList<>();
    public List<Modifier> modifierTraitsT = new ObjectArrayList<>();

    public void registerModifiers() {
        TraitMagicBreaking traitMagicBreaking = registerModifier(new TraitMagicBreaking());
        traitMagicBreaking.addItem(ItemBasic.getItem(MagicBreaking.MAGICBREAKING.getId() + "_stone"), 1, 1);

        modifierTraitsF.add(traitMagicBreaking);
    }
}