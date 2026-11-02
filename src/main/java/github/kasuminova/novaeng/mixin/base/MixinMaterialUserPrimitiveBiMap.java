package github.kasuminova.novaeng.mixin.base;

import com.google.common.collect.BiMap;
import com.teamacronymcoders.base.IBaseMod;
import com.teamacronymcoders.base.materialsystem.MaterialException;
import com.teamacronymcoders.base.materialsystem.MaterialSystem;
import com.teamacronymcoders.base.materialsystem.MaterialUser;
import com.teamacronymcoders.base.materialsystem.materialparts.MaterialPart;
import com.teamacronymcoders.base.materialsystem.materialparts.MaterialPartSave;
import com.teamacronymcoders.base.savesystem.SaveLoader;

import github.kasuminova.novaeng.common.util.collection.Int2ObjectBiMap;
import github.kasuminova.novaeng.common.util.collection.Int2ObjectHashBiMap;
import github.kasuminova.novaeng.common.util.collection.PrimitiveBiMaps;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = MaterialUser.class, remap = false)
public abstract class MixinMaterialUserPrimitiveBiMap {
    @Shadow
    private BiMap<Integer, MaterialPart> materialPartBiMap;
    @Shadow
    private Map<String, Integer> nameMapping;
    @Shadow
    private int nextId;
    @Shadow
    @Final
    private IBaseMod mod;

    @Redirect(
        method = {"<init>"},
        at =
        @At(
            value = "FIELD",
            target =
                "Lcom/teamacronymcoders/base/materialsystem/MaterialUser;materialPartBiMap:Lcom/google/common/collect/BiMap;",
            opcode = Opcodes.PUTFIELD),
        require = 1)
    private void nova$primitiveTable(
        final MaterialUser owner, final BiMap<Integer, MaterialPart> original) {
        final Int2ObjectHashBiMap<MaterialPart> replacement = new Int2ObjectHashBiMap<>(16);
        replacement.putAll(original);
        materialPartBiMap = replacement;
    }

    @Shadow
    public abstract void logError(String message);

    /**
     * @author NovaEngineering
     * @reason Preserve the missing material fallback without boxing IDs.
     */
    @Overwrite
    public MaterialPart getMaterialPart(final int itemDamage) {
        return (MaterialPart)
            PrimitiveBiMaps.getIntKeyOrDefault(
                materialPartBiMap, itemDamage, MaterialSystem.MISSING_MATERIAL_PART);
    }

    /**
     * @author NovaEngineering
     * @reason Resolve material IDs through the primitive inverse.
     */
    @Overwrite
    public int getMaterialPartId(final MaterialPart part) {
        return PrimitiveBiMaps.getIntValue(materialPartBiMap.inverse(), part);
    }

    /**
     * @author NovaEngineering
     * @reason Store material IDs without temporary boxes at registration.
     */
    @Overwrite
    public void registerMaterialPart(final MaterialPart part) {
        final int id =
            nameMapping.containsKey(part.getUnlocalizedName())
                ? nameMapping.get(part.getUnlocalizedName())
                : nextId++;
        if (!MaterialSystem.hasMaterialPart(part)) {
            PrimitiveBiMaps.putIntObject(materialPartBiMap, id, part);
            MaterialSystem.registerMaterialPart(part);
        } else logError(part.getUnlocalizedName() + " is already registered.");
    }

    @Shadow
    protected abstract boolean hasErred();

    /**
     * @author NovaEngineering
     * @reason Serialize material mappings from primitive entries without boxed pairs.
     */
    @Overwrite
    public void finishUp() {
        final var parts = new ObjectArrayList<>(materialPartBiMap.values());
        for (MaterialPart part : parts) {
            try {
                part.getData().validate();
            } catch (MaterialException failure) {
                logError(
                    "MaterialPart with name "
                        + part.getLocalizedName()
                        + " had an error when validating data. Error: "
                        + failure.getMessage());
            }
            part.setup();
        }
        final MaterialPartSave save;
        if (hasErred()) {
            logError("Found Errors with Material System Loading. Saving Back up");
            save = MaterialPartSave.of(nameMapping);
        } else {
            final var mappings = new Object2IntOpenHashMap<String>(materialPartBiMap.size());
            for (Int2ObjectMap.Entry<MaterialPart> entry :
                ((Int2ObjectBiMap<MaterialPart>) materialPartBiMap).int2ObjectEntrySet()) {
                final String name = entry.getValue().getUnlocalizedName();
                if (mappings.containsKey(name))
                    throw new IllegalStateException("Duplicate key " + name);
                mappings.put(name, entry.getIntKey());
            }
            save = MaterialPartSave.of(mappings);
        }
        SaveLoader.saveObject("material_parts_" + mod.getID(), save);
    }
}
