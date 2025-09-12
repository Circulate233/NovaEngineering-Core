package github.kasuminova.novaeng.common.crafttweaker.expansion;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils;
import github.kasuminova.novaeng.common.machine.IllumPool;
import github.kasuminova.novaeng.common.util.Function;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenExpansion("mods.modularmachinery.RecipePrimer")
public class RecipePrimerIllumPool {

    @ZenMethod
    public static RecipePrimer addIllumPoolManaAddHandler(RecipePrimer primer, int amount) {
        primer.addPreCheckHandler(event -> IllumPool.onAddManaRecipeCheck(event, amount));
        primer.addFactoryPreTickHandler(event -> IllumPool.onAddManaRecipeTick(event, amount));
        primer.addRecipeTooltip(Function.getText("novaeng.illum_pool.recipe.mana", NovaEngUtils.formatDecimal(amount)));
        return primer;
    }

    @ZenMethod
    public static RecipePrimer addIllumPoolIllumAddHandler(RecipePrimer primer, int amount) {
        primer.addPreCheckHandler(event -> IllumPool.onAddIllumRecipeCheck(event, amount));
        primer.addFactoryPreTickHandler(event -> IllumPool.onAddIllumRecipeTick(event, amount));
        primer.addRecipeTooltip(Function.getText("novaeng.illum_pool.recipe.illum", NovaEngUtils.formatDecimal(amount)));
        return primer;
    }

}