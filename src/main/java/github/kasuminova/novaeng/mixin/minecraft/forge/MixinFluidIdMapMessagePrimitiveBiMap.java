package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.google.common.collect.BiMap;

import github.kasuminova.novaeng.common.util.collection.Object2IntHashBiMap;
import github.kasuminova.novaeng.common.util.collection.PrimitiveBiMaps;

import io.netty.buffer.ByteBuf;

import net.minecraftforge.common.network.ForgeMessage.FluidIdMapMessage;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.Set;

@Mixin(value = FluidIdMapMessage.class, remap = false)
public abstract class MixinFluidIdMapMessagePrimitiveBiMap {
    @Shadow
    BiMap<Fluid, Integer> fluidIds;
    @Shadow
    Set<String> defaultFluids;

    @Redirect(
        method = "<init>",
        at =
        @At(
            value = "FIELD",
            target =
                "Lnet/minecraftforge/common/network/ForgeMessage$FluidIdMapMessage;fluidIds:Lcom/google/common/collect/BiMap;",
            opcode = Opcodes.PUTFIELD),
        require = 1)
    private void nova$primitiveIds(
        final FluidIdMapMessage owner, final BiMap<Fluid, Integer> original) {
        fluidIds = Object2IntHashBiMap.copyOf(original);
    }

    /**
     * @author NovaEngineering
     * @reason Decode directly into the primitive fluid table.
     */
    @Overwrite
    void fromBytes(final ByteBuf bytes) {
        final int count = bytes.readInt();
        for (int i = 0; i < count; i++) {
            final String name = ByteBufUtils.readUTF8String(bytes);
            PrimitiveBiMaps.putObjectInt(fluidIds, FluidRegistry.getFluid(name), bytes.readInt());
        }
        if (bytes.isReadable()) {
            for (int i = 0; i < count; i++) defaultFluids.add(ByteBufUtils.readUTF8String(bytes));
        } else {
            FMLLog.log.info(
                "Legacy server message contains no default fluid list - there may be problems"
                    + " with fluids");
            defaultFluids.clear();
        }
    }

    /**
     * @author NovaEngineering
     * @reason Encode numeric IDs through native entries.
     */
    @Overwrite
    void toBytes(final ByteBuf bytes) {
        final BiMap<Fluid, Integer> ids = AccessorFluidRegistryIds.nova$getFluidIds();
        bytes.writeInt(ids.size());
        for (final Map.Entry<Fluid, Integer> entry : ids.entrySet()) {
            ByteBufUtils.writeUTF8String(bytes, entry.getKey().getName());
            bytes.writeInt(PrimitiveBiMaps.entryIntValue(entry));
        }
        for (final Fluid fluid : ids.keySet())
            ByteBufUtils.writeUTF8String(bytes, FluidRegistry.getDefaultFluidName(fluid));
    }
}
