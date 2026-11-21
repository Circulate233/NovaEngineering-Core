package github.kasuminova.novaeng.mixin.enderio;

import com.enderio.core.common.util.NNList;
import crazypants.enderio.base.Log;
import crazypants.enderio.base.config.recipes.xml.AbstractConditional;
import crazypants.enderio.base.config.recipes.xml.Recipes;
import crazypants.enderio.base.recipe.RecipeLevel;
import net.minecraftforge.fml.common.ProgressManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(value = Recipes.class, remap = false)
public abstract class MixinRecipesRegistration {
    @Shadow
    @Final
    protected NNList<AbstractConditional> recipes;

    /**
     * @author circulation
     * @reason Reuse each recipe's progress label for registration instead of concatenating it twice.
     */
    @Overwrite
    public void register(String name, RecipeLevel level) {
        String prefix = name.isEmpty() ? "" : name + ":";
        Log.debug("Starting registering XML recipes");
        ProgressManager.ProgressBar bar = ProgressManager.push("Recipe", recipes.size());
        for (AbstractConditional recipe : recipes) {
            String recipeName = recipe.getName();
            String label = prefix + recipeName;
            bar.step(label);
            // Preserve both name callbacks and their position around the progress callback.
            String registrationName = recipe.getName();
            recipe.register(Objects.equals(recipeName, registrationName) ? label : prefix + registrationName, level);
        }
        ProgressManager.pop(bar);
        Log.debug("Done registering XML recipes");
    }
}
