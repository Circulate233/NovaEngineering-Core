package github.kasuminova.novaeng.common.Trait;

import github.kasuminova.novaeng.common.Enchantment.MagicBreaking;
import net.minecraft.nbt.NBTTagCompound;
import slimeknights.tconstruct.library.modifiers.ModifierAspect;
import slimeknights.tconstruct.library.modifiers.ModifierTrait;
import slimeknights.tconstruct.library.tinkering.Category;
import slimeknights.tconstruct.library.utils.ToolBuilder;

public class TraitMagicBreaking extends ModifierTrait {

    public TraitMagicBreaking() {
        super("magic_breaking", 0x8470FF, 1, 0);
        aspects.clear();
        addAspects(new ModifierAspect.CategoryAnyAspect(Category.WEAPON, Category.LAUNCHER));
    }

    @Override
    public void applyEffect(NBTTagCompound rootCompound, NBTTagCompound modifierTag) {
        super.applyEffect(rootCompound, modifierTag);
        ToolBuilder.addEnchantment(rootCompound, MagicBreaking.MAGICBREAKING);
    }

}
