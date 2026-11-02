package github.kasuminova.novaeng.common.util;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.minecraft.CraftTweakerMC;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import hellfirepvp.modularmachinery.common.machine.IOType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.translation.I18n;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenExpansion("mods.modularmachinery.RecipePrimer")
public final class RecipePrimerEx {

    private RecipePrimerEx() {
    }

    @ZenMethod
    public static RecipePrimer setItemTags(final RecipePrimer recipe, final String tagName, final boolean input) {
        if (recipe instanceof NovaRAB tags) {
            if (input) {
                tags.n$setInTags(tagName);
            } else {
                tags.n$setOutTags(tagName);
            }
        }
        for (final var component : recipe.getComponents()) {
            if (component instanceof RequirementItem item) {
                if (item.getActionType() == IOType.OUTPUT && input
                    || item.getActionType() == IOType.INPUT && !input) {
                    continue;
                }
                item.setTag(new ComponentSelectorTag(tagName));
            }
        }
        return recipe;
    }

    @ZenMethod
    public static RecipePrimer setMultipleParallelized(final RecipePrimer recipe, final int multiple) {
        recipe.addPreCheckHandler(event -> event.getActiveRecipe().setMaxParallelism(
            event.getActiveRecipe().getMaxParallelism() * multiple));
        return recipe;
    }

    @ZenMethod
    @SuppressWarnings("deprecation")
    public static RecipePrimer setLore(final RecipePrimer recipe, final String... keys) {
        final NBTTagCompound tag = new NBTTagCompound();
        final NBTTagCompound display = new NBTTagCompound();
        final NBTTagList lore = new NBTTagList();
        for (final String key : keys) {
            lore.appendTag(new NBTTagString(I18n.translateToLocalFormatted(key)));
        }
        display.setTag("Lore", lore);
        tag.setTag("display", display);
        recipe.setPreViewNBT(CraftTweakerMC.getIData(tag));
        return recipe;
    }
}
