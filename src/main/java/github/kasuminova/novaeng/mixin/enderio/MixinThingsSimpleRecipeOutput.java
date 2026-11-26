package github.kasuminova.novaeng.mixin.enderio;

import com.enderio.core.common.util.NNList;
import com.enderio.core.common.util.stackable.Things;
import github.kasuminova.novaeng.common.performance.SingleRecipeOutput;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(value = Things.class, remap = false)
public abstract class MixinThingsSimpleRecipeOutput implements SingleRecipeOutput {
    @Shadow
    private static boolean inPreInit;
    @Shadow
    private static boolean beforeLoadComplete;
    @Shadow
    @Final
    private List<?> things;
    @Shadow
    private NBTTagCompound nbt;
    @Shadow
    private int size;

    @Shadow
    private void applyNBT(final NNList<ItemStack> stacks) {
        throw new AssertionError();
    }

    @Override
    @Nullable
    public ItemStack nova$singleRecipeOutput() {
        // During preInit validation bakes a temporary copy. After loadComplete Things caches its lists.
        if (inPreInit || !beforeLoadComplete || !getClass().equals(Things.class) || things.size() != 1) return null;
        final Object thing = things.getFirst();
        if (!(thing instanceof AccessorItemStackThing access)
            || !thing.getClass().getName().equals("com.enderio.core.common.util.stackable.ItemStackThing")) return null;

        final ItemStack stack = access.nova$getStack();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        final boolean wildcard = stack.getMetadata() == OreDictionary.WILDCARD_VALUE;
        if (!wildcard && nbt == null && size <= 1) return stack;

        final NNList<ItemStack> results = new NNList<>();
        if (wildcard) {
            stack.getItem().getSubItems(CreativeTabs.SEARCH, results);
        } else {
            results.add(stack);
        }
        applyNBT(results);
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0);
    }
}
