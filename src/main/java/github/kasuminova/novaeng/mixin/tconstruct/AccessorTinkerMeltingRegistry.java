package github.kasuminova.novaeng.mixin.tconstruct;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.smeltery.MeltingRecipe;

import java.util.List;

@Mixin(value = TinkerRegistry.class, remap = false)
public interface AccessorTinkerMeltingRegistry {
    @Accessor("meltingRegistry")
    static List<MeltingRecipe> nova$meltingRecipes() {
        throw new AssertionError();
    }
}
