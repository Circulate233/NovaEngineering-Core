package github.kasuminova.novaeng.common.util;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.data.IData;
import crafttweaker.api.minecraft.CraftTweakerMC;
import github.kasuminova.novaeng.mixin.util.NovaRAB;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.translation.I18n;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenExpansion("mods.modularmachinery.RecipePrimer")
public class RecipePrimerEx {

    @ZenMethod
    public static RecipePrimer setItemTags(final RecipePrimer primer, final String tagName, boolean isInput) {
        if (primer instanceof NovaRAB n) {
            if (isInput) {
                n.n$setInTags(tagName);
            } else {
                n.n$setOutTags(tagName);
            }
        }

        for (ComponentRequirement<?, ?> component : primer.getComponents()) {
            if (component instanceof RequirementItem) {
                switch (component.getActionType()) {
                    case OUTPUT -> {
                        if (isInput) continue;
                    }
                    case INPUT -> {
                        if (!isInput) continue;
                    }
                }
                var tag = new ComponentSelectorTag(tagName);
                component.setTag(tag);
            }
        }
        return primer;
    }

    @ZenMethod
    public static RecipePrimer setMultipleParallelized(final RecipePrimer primer, final int multipe) {
        primer.addPreCheckHandler(event -> event.getActiveRecipe().setMaxParallelism((event.getActiveRecipe().getMaxParallelism() * multipe)));
        return primer;
    }

    @ZenMethod
    public static RecipePrimer setLore(final RecipePrimer primer, String... key) {
        primer.setPreViewNBT(getData(key));
        return primer;
    }

    private static IData getData(String... key) {
        var nbt = new NBTTagCompound();
        var nbt1 = new NBTTagCompound();
        var list = new NBTTagList();
        for (String s : key) {
            list.appendTag(new NBTTagString(I18n.translateToLocalFormatted(s)));
        }
        nbt1.setTag("Lore", list);
        nbt.setTag("display", nbt1);
        return CraftTweakerMC.getIData(nbt);
    }

}
