package github.kasuminova.novaeng.common.trait;

import github.kasuminova.novaeng.common.enchantment.MagicBreaking;
import net.minecraft.nbt.NBTTagCompound;
import slimeknights.tconstruct.library.modifiers.ModifierAspect;
import slimeknights.tconstruct.library.utils.ToolBuilder;
import slimeknights.tconstruct.tools.modifiers.ToolModifier;

public class TraitMagicBreaking extends ToolModifier {

    public TraitMagicBreaking() {
        super("magic_breaking", 0x8470FF);
        addAspects(new ModifierAspect.SingleAspect(this), new ModifierAspect.DataAspect(this), ModifierAspect.freeModifier);
    }

    @Override
    public void applyEffect(NBTTagCompound rootCompound, NBTTagCompound modifierTag) {
        ToolBuilder.addEnchantment(rootCompound, MagicBreaking.MAGICBREAKING);
    }

}
