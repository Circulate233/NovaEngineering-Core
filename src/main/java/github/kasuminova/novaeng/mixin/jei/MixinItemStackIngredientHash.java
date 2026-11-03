package github.kasuminova.novaeng.mixin.jei;

import github.kasuminova.novaeng.common.util.collection.WeakObject2IntCache;
import mezz.jei.plugins.vanilla.ingredients.item.ItemStackHelper;
import mezz.jei.util.Log;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ItemStackHelper.class, remap = false)
public abstract class MixinItemStackIngredientHash {

    @Unique
    private final WeakObject2IntCache<ItemStack> nova$hashes = new WeakObject2IntCache<>();

    /**
     * @author circulation
     * @reason Cache primitive hashes without boxing values or retaining temporary ingredient stacks.
     */
    @Overwrite
    public int getHash(final ItemStack ingredient) {
        final long cached = this.nova$hashes.getCached(ingredient);
        if (cached != WeakObject2IntCache.ABSENT) return (int) cached;
        int hash = ingredient.getCount() * 31 + ingredient.getItemDamage();
        hash = hash * 31 + ingredient.getItem().getRegistryName().hashCode();
        final NBTTagCompound tag = ingredient.getTagCompound();
        if (tag != null) {
            try {
                hash = hash * 31 + tag.hashCode();
            } catch (StackOverflowError e) {
                Log.get().error("Stack overflow while hashing ItemStack ingredient: {}", ingredient.getItem().getRegistryName(), e);
            }
        }
        this.nova$hashes.put(ingredient, hash);
        return hash;
    }
}
