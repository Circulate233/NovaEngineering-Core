package github.kasuminova.novaeng.mixin.crafttweaker;

import com.blamejared.ctgui.api.GuiRegistry;
import com.google.common.collect.BiMap;

import github.kasuminova.novaeng.common.util.collection.Object2IntHashBiMap;
import github.kasuminova.novaeng.common.util.collection.PrimitiveBiMaps;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GuiRegistry.class, remap = false)
public abstract class MixinGuiRegistryPrimitiveBiMap {
    @Shadow
    public static BiMap<String, Integer> guiMap;

    @Redirect(
        method = {"<clinit>"},
        at =
        @At(
            value = "FIELD",
            target =
                "Lcom/blamejared/ctgui/api/GuiRegistry;guiMap:Lcom/google/common/collect/BiMap;",
            opcode = Opcodes.PUTSTATIC),
        require = 1)
    private static void nova$primitiveTable(final BiMap<String, Integer> original) {
        final Object2IntHashBiMap<String> replacement = new Object2IntHashBiMap<>(16);
        replacement.putAll(original);
        guiMap = replacement;
    }

    /**
     * @author NovaEngineering
     * @reason Register primitive GUI IDs.
     */
    @Overwrite
    public static void registerGui(final String name, final int ID) {
        PrimitiveBiMaps.putObjectInt(guiMap, name, ID);
    }

    /**
     * @author NovaEngineering
     * @reason Preserve the missing-GUI sentinel with a primitive lookup.
     */
    @Overwrite
    public static int getID(final String name) {
        return PrimitiveBiMaps.getIntValueOrDefault(guiMap, name, Integer.MIN_VALUE);
    }

    /**
     * @author NovaEngineering
     * @reason Resolve GUI names without boxing the ID.
     */
    @Overwrite
    public static String getName(final int id) {
        return (String) PrimitiveBiMaps.getIntKey(guiMap.inverse(), id);
    }
}
