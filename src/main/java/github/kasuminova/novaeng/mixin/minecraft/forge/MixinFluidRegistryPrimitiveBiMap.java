package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import github.kasuminova.novaeng.common.util.collection.Int2ObjectHashBiMap;
import github.kasuminova.novaeng.common.util.collection.Object2IntHashBiMap;
import github.kasuminova.novaeng.common.util.collection.PrimitiveBiMaps;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidRegistry.FluidRegisterEvent;
import net.minecraftforge.fml.common.FMLLog;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Mixin(value = FluidRegistry.class, remap = false)
public abstract class MixinFluidRegistryPrimitiveBiMap {
    @Shadow
    static BiMap<String, Fluid> fluids;
    @Shadow
    static BiMap<Fluid, Integer> fluidIDs;
    @Shadow
    static BiMap<Integer, String> fluidNames;
    @Shadow
    static BiMap<Block, Fluid> fluidBlocks;
    @Shadow
    static BiMap<String, Fluid> masterFluidReference;
    @Shadow
    static BiMap<String, String> defaultFluidName;
    @Shadow
    static Map<Fluid, FluidRegistry.FluidDelegate> delegates;
    @Shadow
    static int maxID;
    @Shadow
    static Set<Fluid> currentBucketFluids;

    @Shadow
    private static String uniqueName(Fluid fluid) {
        throw new AssertionError();
    }

    @Redirect(
        method = "<clinit>",
        at =
        @At(
            value = "FIELD",
            target =
                "Lnet/minecraftforge/fluids/FluidRegistry;fluidIDs:Lcom/google/common/collect/BiMap;",
            opcode = Opcodes.PUTSTATIC),
        require = 1)
    private static void nova$primitiveIds(final BiMap<Fluid, Integer> original) {
        fluidIDs = Object2IntHashBiMap.copyOf(original);
    }

    @Redirect(
        method = "<clinit>",
        at =
        @At(
            value = "FIELD",
            target =
                "Lnet/minecraftforge/fluids/FluidRegistry;fluidNames:Lcom/google/common/collect/BiMap;",
            opcode = Opcodes.PUTSTATIC),
        require = 1)
    private static void nova$primitiveNames(final BiMap<Integer, String> original) {
        fluidNames = Int2ObjectHashBiMap.copyOf(original);
    }

    /**
     * @author NovaEngineering
     * @reason Preserve incoming map aliases and build primitive name indexes.
     */
    @Overwrite
    private static void loadFluidDefaults(
        final BiMap<Fluid, Integer> localFluidIDs, final Set<String> defaultNames) {
        if (defaultNames.isEmpty()) defaultNames.addAll(defaultFluidName.values());
        final BiMap<String, Fluid> localFluids = HashBiMap.create(fluids);
        for (final String defaultName : defaultNames) {
            Fluid fluid = masterFluidReference.get(defaultName);
            if (fluid == null) {
                final String derivedName = defaultName.split(":", 2)[1];
                final String localDefault = defaultFluidName.get(derivedName);
                if (localDefault == null) {
                    FMLLog.log.error(
                        "The fluid {} (specified as {}) is missing from this instance - it will"
                            + " be removed",
                        derivedName,
                        defaultName);
                    continue;
                }
                fluid = masterFluidReference.get(localDefault);
                FMLLog.log.error(
                    "The fluid {} specified as default is not present - it will be reverted to"
                        + " default {}",
                    defaultName,
                    localDefault);
            }
            FMLLog.log.debug(
                "The fluid {} has been selected as the default fluid for {}",
                defaultName,
                fluid.getName());
            final Fluid oldFluid = localFluids.put(fluid.getName(), fluid);
            PrimitiveBiMaps.moveObjectIntKey(localFluidIDs, oldFluid, fluid);
        }
        final Int2ObjectHashBiMap<String> localNames =
            new Int2ObjectHashBiMap<>(localFluidIDs.size());
        final boolean hasNullId = localFluidIDs.inverse().containsKey(null);
        final Fluid nullIdFluid = hasNullId ? localFluidIDs.inverse().get(null) : null;
        for (final var entry : localFluidIDs.entrySet()) {
            if (hasNullId && Objects.equals(entry.getKey(), nullIdFluid)) {
                localNames.put(null, entry.getKey().getName());
            } else {
                localNames.putDiscard(
                    PrimitiveBiMaps.entryIntValue(entry), entry.getKey().getName(), false);
            }
        }
        fluidIDs = localFluidIDs;
        fluids = localFluids;
        fluidNames = localNames;
        fluidBlocks = null;
        currentBucketFluids = null;
        for (final Object delegate : delegates.values())
            ((InvokerFluidRegistryDelegate) delegate).nova$rebind();
    }

    /**
     * @author NovaEngineering
     * @reason Copy persisted fluid IDs without reconstructing boxed storage.
     */
    @Overwrite
    public static void loadFluidDefaults(final NBTTagCompound tag) {
        final Set<String> defaults = new ObjectOpenHashSet<>();
        if (tag.hasKey("DefaultFluidList", 9)) {
            FMLLog.log.debug("Loading persistent fluid defaults from world");
            final NBTTagList list = tag.getTagList("DefaultFluidList", 8);
            for (int i = 0; i < list.tagCount(); i++) defaults.add(list.getStringTagAt(i));
        } else
            FMLLog.log.debug("World is missing persistent fluid defaults - using local defaults");
        loadFluidDefaults(Object2IntHashBiMap.copyOf(fluidIDs), defaults);
    }

    /**
     * @author NovaEngineering
     * @reason Register both directions of a fluid ID without boxing.
     */
    @Overwrite
    public static boolean registerFluid(final Fluid fluid) {
        masterFluidReference.put(uniqueName(fluid), fluid);
        delegates.put(fluid, new FluidRegistry.FluidDelegate(fluid, fluid.getName()));
        if (fluids.containsKey(fluid.getName())) return false;
        fluids.put(fluid.getName(), fluid);
        PrimitiveBiMaps.putObjectInt(fluidIDs, fluid, ++maxID);
        PrimitiveBiMaps.putIntObject(fluidNames, maxID, fluid.getName());
        defaultFluidName.put(fluid.getName(), uniqueName(fluid));
        MinecraftForge.EVENT_BUS.post(new FluidRegisterEvent(fluid.getName(), maxID));
        return true;
    }
}
