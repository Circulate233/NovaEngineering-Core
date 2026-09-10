package github.kasuminova.novaeng.common.enchantment;

import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class MagicBreaking extends Enchantment {

    public static MagicBreaking MAGICBREAKING = new MagicBreaking();

    public MagicBreaking() {
        super(Rarity.VERY_RARE, EnumEnchantmentType.ALL, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND});
        this.setName("magic_breaking");
        this.setRegistryName(new ResourceLocation(Tags.MOD_ID, name));
    }

    @Override
    public boolean isTreasureEnchantment() {
        return true;
    }

    public @NotNull String getId() {
        return this.name;
    }

}
