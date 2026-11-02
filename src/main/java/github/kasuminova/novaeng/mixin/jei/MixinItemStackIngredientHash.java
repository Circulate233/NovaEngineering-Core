package github.kasuminova.novaeng.mixin.jei;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import mezz.jei.plugins.vanilla.ingredients.item.ItemStackHelper;
import mezz.jei.util.Log;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ItemStackHelper.class, remap = false)
public abstract class MixinItemStackIngredientHash {

    @Shadow
    @Final
    private Object2IntMap<ItemStack> hashCache;

    /**
     * @author circulation
     * @reason Read cached primitive hashes without boxing them as Integer on every ingredient visit.
     */
    @Overwrite
    public int getHash(final ItemStack ingredient) {
        final int cached = this.hashCache.getInt(ingredient);
        if (cached != this.hashCache.defaultReturnValue() || this.hashCache.containsKey(ingredient)) {
            return cached;
        }
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
        this.hashCache.put(ingredient, hash);
        return hash;
    }
}
