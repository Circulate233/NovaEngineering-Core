package github.kasuminova.novaeng.mixin.bogosorter;

import com.cleanroommc.bogosorter.common.OreDictHelper;
import com.cleanroommc.bogosorter.common.config.BogoSorterConfig;
import github.kasuminova.novaeng.common.performance.BogoOreNameParser;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Set;

@Mixin(value = OreDictHelper.class, remap = false)
public abstract class MixinOreDictHelper {
    @Shadow
    @Final
    private static Map<ItemStack, Set<String>> ORE_DICTS;
    @Shadow
    @Final
    private static Map<ItemStack, String> MATERIALS;
    @Shadow
    @Final
    private static Map<ItemStack, String> PREFIXES;

    /**
     * @author NovaEngineering
     * @reason Scan token boundaries directly instead of constructing token lists and joining suffixes.
     */
    @SubscribeEvent
    @Overwrite
    public static void onItemRegistration(final OreDictionary.OreRegisterEvent event) {
        ORE_DICTS.computeIfAbsent(event.getOre(), ignored -> new ObjectOpenHashSet<>()).add(event.getName());
        final String name = event.getName();
        final String prefix = BogoOreNameParser.findPrefix(name, candidate -> BogoSorterConfig.ORE_PREFIXES.containsKey(candidate));
        if (prefix != null && BogoSorterConfig.ORE_PREFIXES.containsKey(prefix)) {
            MATERIALS.put(event.getOre(), name.substring(prefix.length()));
            PREFIXES.put(event.getOre(), prefix);
        }
    }
}
