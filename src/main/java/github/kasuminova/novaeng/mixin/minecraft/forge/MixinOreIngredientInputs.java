package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.common.performance.OreIngredientInputs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = OreIngredient.class, remap = false)
public abstract class MixinOreIngredientInputs implements OreIngredientInputs {
    @Shadow
    private NonNullList<ItemStack> ores;

    @Override
    public List<ItemStack> nova$rawOreInputs() {
        return ores;
    }
}
