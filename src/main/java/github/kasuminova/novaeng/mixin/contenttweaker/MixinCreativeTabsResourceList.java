package github.kasuminova.novaeng.mixin.contenttweaker;

import com.teamacronymcoders.contenttweaker.api.utils.CreativeTabsResourceList;
import github.kasuminova.novaeng.common.util.CreativeTabLookupCache;
import net.minecraft.creativetab.CreativeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Replaces ContentTweaker's creative tab name lookup with a memoised one.
 *
 * <p>The lookup and the state it needs live in {@link CreativeTabLookupCache}: a mixin's own declarations are
 * merged into its target, so keeping them out of here keeps the target's class initialiser untouched.</p>
 */
@Mixin(value = CreativeTabsResourceList.class, remap = false)
public class MixinCreativeTabsResourceList {

    /**
     * @author circulation
     * @reason The original resolves every tab's label by reflection on every call; the cache keeps the same
     *         first-match behaviour over the same array while reading each label once and remembering the answer
     *         for a name.
     */
    @Overwrite(remap = false)
    public CreativeTabs getResource(final String name) {
        return CreativeTabLookupCache.find(name);
    }

}
