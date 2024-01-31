package github.kasuminova.novaeng.mixin.techguns;

import com.google.gson.JsonObject;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.ShapedOreRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.items.guns.GenericGun;
import techguns.recipes.AmmoSumRecipeFactory;

import javax.annotation.Nonnull;

@Mixin(AmmoSumRecipeFactory.class)
public class MixinAmmoSumRecipe {

    @Shadow(remap = false) @Final protected static ResourceLocation GROUP;

    /**
     * @author Kasumi_Nova
     * @reason getCraftingResult mixin 不掉，那就只能 extend 一个了捏。
     */
    @Overwrite(remap = false)
    public IRecipe parse(JsonContext context, JsonObject json) {
        ShapedOreRecipe rec = ShapedOreRecipe.factory(context, json);
        CraftingHelper.ShapedPrimer primer = new CraftingHelper.ShapedPrimer();
        primer.height = rec.getRecipeHeight();
        primer.width = rec.getRecipeWidth();
        primer.mirrored = JsonUtils.getBoolean(json, "mirrored", true);
        primer.input = rec.getIngredients();

        return new ShapedOreRecipe(GROUP, rec.getRecipeOutput(), primer) {
            @Nonnull
            @Override
            public ItemStack getCraftingResult(@Nonnull final InventoryCrafting inv) {
                int ammoSum = 0;

                for (int i = 0; i < inv.getSizeInventory(); ++i) {
                    if (!inv.getStackInSlot(i).isEmpty() && inv.getStackInSlot(i).getItem() instanceof final GenericGun g) {
                        ammoSum += g.getCurrentAmmo(inv.getStackInSlot(i));
                    }
                }

                ItemStack out = super.getCraftingResult(inv);
                NBTTagCompound tags = out.getTagCompound();
                if (tags == null) {
                    tags = new NBTTagCompound();
                    out.setTagCompound(tags);
                }
                tags.setShort("ammo", (short) ammoSum);
                return out;
            }
        };
    }

}
