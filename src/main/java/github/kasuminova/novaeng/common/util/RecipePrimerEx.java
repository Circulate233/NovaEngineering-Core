package github.kasuminova.novaeng.common.util;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenExpansion("mods.modularmachinery.RecipePrimer")
public class RecipePrimerEx {

    @ZenMethod
    public static RecipePrimer setItemTags(final RecipePrimer primer, final String tagName ,boolean isInput) {
        if (primer instanceof NovaRAB n){
            if (isInput) {
                n.n$setInTags(tagName);
            } else {
                n.n$setOutTags(tagName);
            }
        }

        for (ComponentRequirement<?, ?> component : primer.getComponents()) {
            if (component instanceof RequirementItem) {
                switch (component.getActionType()) {
                    case OUTPUT:
                        if (isInput) continue;
                    case INPUT:
                        if (!isInput) continue;
                }
                var tag = new ComponentSelectorTag(tagName);
                component.setTag(tag);
            }
        }
        return primer;
    }
}
