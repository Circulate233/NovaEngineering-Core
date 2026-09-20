package github.kasuminova.novaeng.mixin.crafttweaker;

import crafttweaker.mc1120.CraftTweaker;
import crafttweaker.mc1120.oredict.MCOreDictEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.OreIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves an ore ingredient's dictionary name through an identity index instead of rescanning every name.
 *
 * <p>The original walks the whole ore name table for every ingredient and compares the ingredient's captured
 * list by reference. That comparison is load bearing rather than incidental: {@code getOres(name, false)}
 * returns the very list stored in the dictionary, so list identity is exactly what identifies a name. This
 * keeps the comparison and its first-match ordering, and only removes the repeated full traversal.</p>
 *
 * <p>The index is rebuilt when the name count changes. {@code OreDictionary.registerOreImpl} appends into the
 * existing per-name list instead of replacing it, and names are only ever added, so an unchanged count means
 * an unchanged mapping and a miss means the original scan would have missed as well. When the dictionary
 * internals cannot be inspected the original scan still runs, so the answer stays correct either way.</p>
 */
@Mixin(value = MCOreDictEntry.class, remap = false)
public class MixinMCOreDictEntry {

    @Unique
    private static volatile Map<NonNullList<ItemStack>, String> nova$namesByOres;
    @Unique
    private static volatile int nova$indexedNameCount = -1;
    @Unique
    private static volatile Field nova$oresField;
    @Unique
    private static volatile List<?> nova$idToName;
    @Unique
    private static volatile boolean nova$reflectionResolved;

    /**
     * @author circulation
     * @reason resolve the captured ore list to its dictionary name through an identity index, preserving the
     *         original reference comparison and first-match ordering while removing the per-call full scan
     */
    @SuppressWarnings("unchecked")
    @Overwrite(remap = false)
    public static MCOreDictEntry getFromIngredient(final OreIngredient ingredient) {
        final Field oresField = nova$resolveReflection();
        if (oresField == null) {
            return null;
        }

        final NonNullList<ItemStack> captured;
        try {
            captured = (NonNullList<ItemStack>) oresField.get(ingredient);
        } catch (final IllegalAccessException failure) {
            CraftTweaker.LOG.catching(failure);
            return null;
        }
        if (captured == null) {
            return null;
        }

        final List<?> idToName = nova$idToName;
        if (idToName == null) {
            return nova$scanByName(captured);
        }

        final int count = idToName.size();
        Map<NonNullList<ItemStack>, String> index = nova$namesByOres;
        if (index == null || nova$indexedNameCount != count) {
            index = nova$buildIndex();
            nova$namesByOres = index;
            nova$indexedNameCount = count;
        }

        final String name = index.get(captured);
        return name == null ? null : new MCOreDictEntry(name);
    }

    @Unique
    private static MCOreDictEntry nova$scanByName(final NonNullList<ItemStack> captured) {
        for (final String name : OreDictionary.getOreNames()) {
            if (OreDictionary.getOres(name, false) == captured) {
                return new MCOreDictEntry(name);
            }
        }
        return null;
    }

    @Unique
    private static Map<NonNullList<ItemStack>, String> nova$buildIndex() {
        final String[] names = OreDictionary.getOreNames();
        final Map<NonNullList<ItemStack>, String> index = new IdentityHashMap<>(names.length * 2);
        for (final String name : names) {
            if (name != null) {
                index.putIfAbsent(OreDictionary.getOres(name, false), name);
            }
        }
        return index;
    }

    @Unique
    private static Field nova$resolveReflection() {
        if (!nova$reflectionResolved) {
            try {
                final Field field = OreIngredient.class.getDeclaredField("ores");
                field.setAccessible(true);
                nova$oresField = field;
            } catch (final NoSuchFieldException | RuntimeException failure) {
                CraftTweaker.LOG.catching(failure);
            }
            try {
                final Field field = OreDictionary.class.getDeclaredField("idToName");
                field.setAccessible(true);
                nova$idToName = (List<?>) field.get(null);
            } catch (final NoSuchFieldException | IllegalAccessException | RuntimeException failure) {
                CraftTweaker.LOG.catching(failure);
            }
            nova$reflectionResolved = true;
        }
        return nova$oresField;
    }
}
