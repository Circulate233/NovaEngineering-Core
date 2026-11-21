package github.kasuminova.novaeng.common.performance;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import slimeknights.tconstruct.library.utils.TagUtil;

/**
 * Read-only queries never need TagUtil's fresh, writable empty compounds and lists.
 */
public final class TinkerTagQueries {
    private TinkerTagQueries() {
    }

    public static NBTTagCompound stackTag(final ItemStack stack) {
        // Same guards and evaluation order as TagUtil.getTagSafe, with no empty-object allocation.
        return stack == null || stack.getItem() == null || stack.isEmpty() || !stack.hasTagCompound()
            ? null : stack.getTagCompound();
    }

    public static boolean hasTrait(final NBTTagCompound tag, final String identifier) {
        if (tag == null) return false;
        if (tag.getClass() != NBTTagCompound.class)
            return contains(TagUtil.getTraitsTagList(tag), identifier);
        return contains(stringList(tag, "Traits"), identifier);
    }

    public static boolean hasBaseModifier(final NBTTagCompound tag, final String identifier) {
        if (tag == null) return false;
        if (tag.getClass() != NBTTagCompound.class)
            return contains(TagUtil.getBaseModifiersTagList(tag), identifier);
        final NBTBase base = tag.getTag("TinkerData");
        if (!(base instanceof NBTTagCompound compound)) return false;
        if (compound.getClass() != NBTTagCompound.class)
            return contains(TagUtil.getBaseModifiersTagList(tag), identifier);
        return contains(stringList(compound, "Modifiers"), identifier);
    }

    private static NBTTagList stringList(final NBTTagCompound tag, final String key) {
        final NBTBase value = tag.getTag(key);
        if (!(value instanceof NBTTagList list)) return null;
        // Preserve virtual getTagList behavior for custom NBT list implementations.
        if (list.getClass() != NBTTagList.class) return tag.getTagList(key, TagUtil.TAG_TYPE_STRING);
        return list.tagCount() == 0 || list.getTagType() == TagUtil.TAG_TYPE_STRING ? list : null;
    }

    private static boolean contains(final NBTTagList list, final String identifier) {
        if (list != null) for (int index = 0; index < list.tagCount(); index++) {
            // Match TinkerUtil: null identifiers only throw for a nonempty, valid list.
            if (identifier.equals(list.getStringTagAt(index))) return true;
        }
        return false;
    }
}
