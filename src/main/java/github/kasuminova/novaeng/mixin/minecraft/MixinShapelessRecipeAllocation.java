package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapelessRecipes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;

@Mixin(ShapelessRecipes.class)
public abstract class MixinShapelessRecipeAllocation {

    // Constructor redirects must return a non-null instance. Complex recipes never access this
    // local: their final isSimple flag routes every operation to the separate input list.
    @Unique
    private static final RecipeItemHelper NOVA_UNUSED_SIMPLE_MATCHER = new RecipeItemHelper();
    @Shadow(remap = false)
    @Final
    private boolean isSimple;

    @Redirect(method = "matches", at = @At(value = "NEW", target = "net/minecraft/client/util/RecipeItemHelper"), require = 1)
    private RecipeItemHelper nova$allocateSimpleMatcher() {
        return this.isSimple ? new RecipeItemHelper() : NOVA_UNUSED_SIMPLE_MATCHER;
    }

    @Redirect(method = "matches", at = @At(value = "INVOKE",
        target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;", remap = false), require = 1)
    private ArrayList<ItemStack> nova$allocateComplexInputs() {
        return this.isSimple ? null : new ArrayList<>();
    }
}
