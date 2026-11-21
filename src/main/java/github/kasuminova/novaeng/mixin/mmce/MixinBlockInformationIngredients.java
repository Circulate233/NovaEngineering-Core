package github.kasuminova.novaeng.mixin.mmce;

import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import ink.ikx.mmce.common.utils.StackUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = BlockArray.BlockInformation.class, remap = false)
public abstract class MixinBlockInformationIngredients {
    @Shadow
    private List<IBlockState> samples;

    /**
     * @author Codex
     * @reason Avoid a stream pipeline for every preview block while retaining live stack conversion and deduplication.
     */
    @Overwrite
    public List<ItemStack> getIngredientList() {
        final List<ItemStack> ingredients = new ObjectArrayList<>();
        for (final IBlockState sample : samples) {
            final ItemStack stack = StackUtils.getStackFromBlockState(sample);
            if (ItemUtils.stackNotInList(ingredients, stack)) {
                ingredients.add(stack);
            }
        }
        return ingredients;
    }

    /**
     * @author Codex
     * @reason Keep world-sensitive GUI ingredients live without allocating stream stages per block.
     */
    @Overwrite
    public List<ItemStack> getIngredientList(final BlockPos pos, final World world) {
        final List<ItemStack> ingredients = new ObjectArrayList<>();
        for (final IBlockState sample : samples) {
            final ItemStack stack = StackUtils.getStackFromBlockState(sample, pos, world);
            if (ItemUtils.stackNotInList(ingredients, stack)) {
                ingredients.add(stack);
            }
        }
        return ingredients;
    }
}
