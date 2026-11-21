package github.kasuminova.novaeng.mixin.tconstruct;

import github.kasuminova.novaeng.common.performance.MeltingRecipeIndex;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.smeltery.MeltingRecipe;
import slimeknights.tconstruct.library.tools.IToolPart;
import slimeknights.tconstruct.plugin.jei.material.MaterialWrapper;

import java.util.List;

@Mixin(value = MaterialWrapper.class, remap = false)
public abstract class MixinMaterialMeltingInputs {
    @Shadow
    @Final
    Material material;

    /**
     * @author Codex
     * @reason Retain indexed candidates and live fluid predicates without a stream pipeline per wrapper.
     */
    @Overwrite
    private List<ItemStack> getInputs() {
        final List<ItemStack> inputs = new ObjectArrayList<>();
        if (material.hasFluid()) {
            for (final MeltingRecipe recipe : MeltingRecipeIndex.candidates(material.getFluid())) {
                if (material.getFluid().equals(recipe.output.getFluid())) {
                    inputs.addAll(recipe.input.getInputs());
                }
            }
        }
        // Keep the original getter calls: custom materials may compute their representative dynamically.
        if (material.getRepresentativeItem() != null && !material.getRepresentativeItem().isEmpty()) {
            inputs.add(material.getRepresentativeItem());
        }
        return inputs;
    }

    /**
     * @author Codex
     * @reason Avoid stream stages while preserving registry order and dynamic tool-part callbacks.
     */
    @Overwrite
    private List<ItemStack> getOutputs() {
        final List<ItemStack> outputs = new ObjectArrayList<>();
        for (final IToolPart part : TinkerRegistry.getToolParts()) {
            if (part.canUseMaterial(material)) {
                outputs.add(part.getItemstackWithMaterial(material));
            }
        }
        return outputs;
    }
}
