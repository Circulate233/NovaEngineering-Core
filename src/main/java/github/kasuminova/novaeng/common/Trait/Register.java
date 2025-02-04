package github.kasuminova.novaeng.common.Trait;

import github.kasuminova.novaeng.common.Enchantment.MagicBreaking;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import slimeknights.tconstruct.tools.AbstractToolPulse;

public class Register extends AbstractToolPulse {

    public static Register TRAITREGISTER = new Register();

    public void registerModifiers() {
        ItemStack enchantedBook = ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(MagicBreaking.MAGICBREAKING,1));
        TraitMagicBreaking traitMagicBreaking = registerModifier(new TraitMagicBreaking());
        traitMagicBreaking.addItem(enchantedBook,1,1);
    }
}
