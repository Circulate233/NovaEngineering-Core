package github.kasuminova.novaeng.common.trait;

import github.kasuminova.novaeng.common.enchantment.MagicBreaking;
import github.kasuminova.novaeng.common.item.ItemBasic;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.tools.AbstractToolPulse;

import java.util.List;

public final class Register extends AbstractToolPulse {

    public static final Register INSTANCE = new Register();
    private final List<Modifier> modifierTraitsF = new ObjectArrayList<>();
    private final List<Modifier> modifierTraitsT = new ObjectArrayList<>();

    private Register() {
    }

    public List<Modifier> getModifierTraitsF() {
        return this.modifierTraitsF;
    }

    public List<Modifier> getModifierTraitsT() {
        return this.modifierTraitsT;
    }

    public void registerModifiers() {
        final var trait = registerModifier(new TraitMagicBreaking());
        trait.addItem(ItemBasic.getItem(MagicBreaking.MAGICBREAKING.getId() + "_stone"), 1, 1);
        this.modifierTraitsF.add(trait);
    }
}
