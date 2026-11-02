package github.kasuminova.novaeng.mixin.cofh;

import cofh.core.util.ItemWrapper;
import cofh.core.util.oredict.OreDictionaryArbiter;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;

import github.kasuminova.novaeng.common.util.collection.Object2IntHashBiMap;
import github.kasuminova.novaeng.common.util.collection.PrimitiveBiMaps;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Map;

@Mixin(value = OreDictionaryArbiter.class, remap = false)
public abstract class MixinOreDictionaryArbiterPrimitiveBiMap {
    @Shadow
    private static BiMap<String, Integer> oreIDs;
    @Shadow
    private static Map<Integer, ArrayList<ItemStack>> oreStacks;
    @Shadow
    private static Map<ItemWrapper, ArrayList<Integer>> stackIDs;
    @Shadow
    private static Map<ItemWrapper, ArrayList<String>> stackNames;

    @Redirect(
        method = {"<clinit>", "initialize"},
        at =
        @At(
            value = "FIELD",
            target =
                "Lcofh/core/util/oredict/OreDictionaryArbiter;oreIDs:Lcom/google/common/collect/BiMap;",
            opcode = Opcodes.PUTSTATIC),
        require = 1)
    private static void nova$primitiveTable(final BiMap<String, Integer> original) {
        final Object2IntHashBiMap<String> replacement =
            new Object2IntHashBiMap<>(oreIDs == null ? 32 : oreIDs.size());
        replacement.putAll(original);
        oreIDs = replacement;
    }

    /**
     * @author NovaEngineering
     * @reason Preserve unknown ore IDs with a primitive lookup.
     */
    @Overwrite
    public static int getOreID(final String name) {
        return PrimitiveBiMaps.getIntValueOrDefault(oreIDs, name, -1);
    }

    /**
     * @author NovaEngineering
     * @reason Resolve ore names without boxing the ID.
     */
    @Overwrite
    public static String getOreName(final int id) {
        final String name = (String) PrimitiveBiMaps.getIntKey(oreIDs.inverse(), id);
        return name == null ? "Unknown" : name;
    }

    /**
     * @author NovaEngineering
     * @reason Write ore dictionary IDs through the primitive BiMap.
     */
    @Overwrite
    public static void registerOreDictionaryEntry(ItemStack stack, String name) {
        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        int id = OreDictionary.getOreID(name);
        PrimitiveBiMaps.putObjectInt(oreIDs, name, id);
        if (!oreStacks.containsKey(id)) {
            oreStacks.put(id, new ArrayList<>());
        }
        oreStacks.get(id).add(stack);
        ItemWrapper item = ItemWrapper.fromItemStack(stack);
        if (!stackIDs.containsKey(item)) {
            stackIDs.put(item, new ArrayList<>());
            stackNames.put(item, new ArrayList<>());
        }
        stackIDs.get(item).add(id);
        stackNames.get(item).add(name);
    }
}
