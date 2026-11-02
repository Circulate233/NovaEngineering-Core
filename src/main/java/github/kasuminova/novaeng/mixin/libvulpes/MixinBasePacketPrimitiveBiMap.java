package github.kasuminova.novaeng.mixin.libvulpes;

import com.google.common.collect.BiMap;

import github.kasuminova.novaeng.common.util.collection.Int2ObjectHashBiMap;
import github.kasuminova.novaeng.common.util.collection.PrimitiveBiMaps;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import zmaster587.libVulpes.network.BasePacket;

@Mixin(value = BasePacket.class, remap = false)
public abstract class MixinBasePacketPrimitiveBiMap {
    @Shadow
    @Final
    @Mutable
    private static BiMap<Integer, Class<? extends BasePacket>> idMap;

    @Redirect(
        method = {"<clinit>"},
        at =
        @At(
            value = "FIELD",
            target =
                "Lzmaster587/libVulpes/network/BasePacket;idMap:Lcom/google/common/collect/BiMap;",
            opcode = Opcodes.PUTSTATIC),
        require = 1)
    private static void nova$primitiveTable(
        final BiMap<Integer, Class<? extends BasePacket>> original) {
        idMap = Int2ObjectHashBiMap.immutableCopyOf(original);
    }

    /**
     * @author NovaEngineering
     * @reason Resolve the packet type without boxing the incoming ID.
     */
    @Overwrite
    public static BasePacket constructPacket(final int packetId)
        throws BasePacket.ProtocolException, InstantiationException, IllegalAccessException {
        final Class<?> type = (Class<?>) PrimitiveBiMaps.getIntKey(idMap, packetId);
        if (type == null)
            throw new BasePacket.ProtocolException("Protocol Exception!  Unknown Packet Id!");
        return (BasePacket) type.newInstance();
    }

    /**
     * @author NovaEngineering
     * @reason Resolve registered packet IDs through the primitive inverse.
     */
    @Overwrite
    public int getPacketId() {
        final Class<?> type = getClass();
        if (idMap.inverse().containsKey(type))
            return PrimitiveBiMaps.getIntValue(idMap.inverse(), type);
        throw new RuntimeException("Packet " + type.getSimpleName() + " is a missing mapping!");
    }
}
