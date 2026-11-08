package github.kasuminova.novaeng.mixin.immersiveengineering;

import blusunrize.immersiveengineering.api.ApiUtils;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ApiUtils.class, remap = false)
public abstract class MixinMetalComponentLookup {
    /**
     * @author circulation
     * @reason Resolve only the input's ore IDs instead of copying every registered ore name.
     */
    @Overwrite
    public static String getMetalComponentType(final ItemStack stack, final String... componentTypes) {
        for (final int id : OreDictionary.getOreIDs(stack)) {
            final String name = OreDictionary.getOreName(id);
            for (final String component : componentTypes) {
                if (name.startsWith(component)) {
                    return component;
                }
            }
        }
        return null;
    }

    /**
     * @author circulation
     * @reason Preserve ore/prefix order without materializing the global name array.
     */
    @Overwrite
    public static String[] getMetalComponentTypeAndMetal(final ItemStack stack, final String... componentTypes) {
        for (final int id : OreDictionary.getOreIDs(stack)) {
            final String name = OreDictionary.getOreName(id);
            for (final String component : componentTypes) {
                if (name.startsWith(component)) {
                    return new String[]{component, name.substring(component.length())};
                }
            }
        }
        return null;
    }
}
