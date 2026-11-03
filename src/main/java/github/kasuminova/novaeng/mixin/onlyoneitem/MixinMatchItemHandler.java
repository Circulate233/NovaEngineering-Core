package github.kasuminova.novaeng.mixin.onlyoneitem;

import com.circulation.only_one_item.conversion.ItemConversionTarget;
import com.circulation.only_one_item.handler.MatchItemHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Set;

@Mixin(value = MatchItemHandler.class, remap = false)
public abstract class MixinMatchItemHandler {

    @Shadow
    @Final
    private static Map<ResourceLocation, Int2ObjectMap<ItemConversionTarget>> itemIdToTargetMap;
    @Shadow
    @Final
    private static Map<ResourceLocation, IntSet> finalItemBlackMap;
    @Shadow
    @Final
    private static Set<String> finalMODIDBlackSet;
    @Shadow
    @Final
    private static Int2ObjectMap<ItemConversionTarget> defaultMap;

    /**
     * @author NovaEngineering
     * @reason Skip blacklist hash lookups when an ordinary registry key has no replacement target.
     */
    @Overwrite
    public static ItemConversionTarget match(final Item item, final int meta) {
        if (item == null) {
            return null;
        }
        final ResourceLocation id = item.getRegistryName();
        if (id == null) {
            return null;
        }
        if (id.getClass() != ResourceLocation.class) {
            // A custom key may override hash/namespace access; retain its call order.
            final IntSet blacklisted = finalItemBlackMap.get(id);
            return (blacklisted == null || !blacklisted.contains(meta)) && !finalMODIDBlackSet.contains(id.getNamespace())
                ? itemIdToTargetMap.getOrDefault(id, defaultMap).get(meta) : null;
        }
        final Int2ObjectMap<ItemConversionTarget> targets = itemIdToTargetMap.get(id);
        if (targets == null) {
            return null;
        }
        final ItemConversionTarget target = targets.get(meta);
        if (target == null) {
            return null;
        }
        final IntSet blacklisted = finalItemBlackMap.get(id);
        return (blacklisted == null || !blacklisted.contains(meta)) && !finalMODIDBlackSet.contains(id.getNamespace())
            ? target : null;
    }
}
