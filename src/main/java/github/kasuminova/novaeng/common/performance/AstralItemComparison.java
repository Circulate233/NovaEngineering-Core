package github.kasuminova.novaeng.common.performance;

import hellfirepvp.astralsorcery.common.util.ItemComparator.Clause;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import static hellfirepvp.astralsorcery.common.util.nbt.NBTComparator.contains;

/**
 * 用局部位掩码表达比较条件，保持原比较器的检查顺序。
 */
public final class AstralItemComparison {
    public static final int ITEM = 1 << Clause.ITEM.ordinal();
    public static final int AMOUNT_EXACT = 1 << Clause.AMOUNT_EXACT.ordinal();
    public static final int AMOUNT_LEAST = 1 << Clause.AMOUNT_LEAST.ordinal();
    public static final int META_STRICT = 1 << Clause.META_STRICT.ordinal();
    public static final int META_WILDCARD = 1 << Clause.META_WILDCARD.ordinal();
    public static final int NBT_STRICT = 1 << Clause.NBT_STRICT.ordinal();
    public static final int NBT_LEAST = 1 << Clause.NBT_LEAST.ordinal();
    public static final int CAPABILITIES_COMPATIBLE = 1 << Clause.CAPABILITIES_COMPATIBLE.ordinal();

    private AstralItemComparison() {
    }

    public static boolean compare(final ItemStack first, final ItemStack second, final int clauses) {
        if ((clauses & ITEM) != 0) {
            if (first.isEmpty() && !second.isEmpty()) return false;
            if (!first.isEmpty() && !first.getItem().equals(second.getItem())) return false;
        }
        if ((clauses & AMOUNT_EXACT) != 0) {
            if (first.getCount() != second.getCount()) return false;
        } else if ((clauses & AMOUNT_LEAST) != 0 && first.getCount() > second.getCount()) return false;
        if ((clauses & META_STRICT) != 0) {
            if (first.getMetadata() != second.getMetadata()) return false;
        } else if ((clauses & META_WILDCARD) != 0 && first.getMetadata() != second.getMetadata()
            && first.getMetadata() != 32767 && second.getItemDamage() != 32767) return false;
        final NBTTagCompound firstCompound = first.hasTagCompound() ? first.getTagCompound() : null;
        final NBTTagCompound secondCompound = second.hasTagCompound() ? second.getTagCompound() : null;
        final boolean firstTag = firstCompound != null && !isTagEmpty(firstCompound);
        final boolean secondTag = secondCompound != null && !isTagEmpty(secondCompound);
        if ((clauses & NBT_STRICT) != 0) {
            if (!firstTag && secondTag) return false;
            if (firstTag && !(secondTag && firstCompound.equals(secondCompound))) return false;
        } else if ((clauses & NBT_LEAST) != 0 && firstTag) {
            if (!secondTag || !contains(firstCompound, secondCompound)) return false;
        }
        return (clauses & CAPABILITIES_COMPATIBLE) == 0 || first.areCapsCompatible(second);
    }

    private static boolean isTagEmpty(final NBTTagCompound tag) {
        for (final String key : tag.getKeySet()) {
            final NBTBase value = tag.getTag(key);
            if (value instanceof NBTTagCompound compound) {
                if (!isTagEmpty(compound)) return false;
            } else if (value instanceof NBTTagList list) {
                if (!isListEmpty(list)) return false;
            } else return false;
        }
        return true;
    }

    private static boolean isListEmpty(final NBTTagList list) {
        if (!list.isEmpty()) {
            if (list.getTagType() != 9 && list.getTagType() != 10) return false;
            for (final NBTBase value : list) {
                if (value instanceof NBTTagCompound compound) {
                    if (!isTagEmpty(compound)) return false;
                } else if (value instanceof NBTTagList nested) {
                    if (!isListEmpty(nested)) return false;
                } else return false;
            }
        }
        return true;
    }
}
