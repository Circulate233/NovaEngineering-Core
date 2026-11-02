package github.kasuminova.novaeng.mixin.minecraft;

import com.google.common.collect.BiMap;

import github.kasuminova.novaeng.common.util.PacketIdAccess;
import github.kasuminova.novaeng.common.util.collection.Int2ObjectHashBiMap;
import github.kasuminova.novaeng.common.util.collection.PrimitiveBiMaps;

import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;

import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(EnumConnectionState.class)
public abstract class MixinEnumConnectionStatePrimitiveBiMap implements PacketIdAccess {
    @Shadow
    @Final
    private Map<EnumPacketDirection, BiMap<Integer, Class<? extends Packet<?>>>> directionMaps;

    /**
     * @author NovaEngineering
     * @reason Build packet tables with primitive IDs.
     */
    @Overwrite
    protected EnumConnectionState registerPacket(
        final EnumPacketDirection direction, final Class<? extends Packet<?>> packetClass) {
        BiMap<Integer, Class<? extends Packet<?>>> map = directionMaps.get(direction);
        if (map == null) {
            map = new Int2ObjectHashBiMap<>();
            directionMaps.put(direction, map);
        }
        if (map.containsValue(packetClass)) {
            final String message =
                direction
                    + " packet "
                    + packetClass
                    + " is already known to ID "
                    + PrimitiveBiMaps.getIntValue(map.inverse(), packetClass);
            LogManager.getLogger().fatal(message);
            throw new IllegalArgumentException(message);
        }
        PrimitiveBiMaps.putIntObject(map, map.size(), packetClass);
        return (EnumConnectionState) (Object) this;
    }

    @Override
    public int nova$getPacketId(final EnumPacketDirection direction, final Packet<?> packet) {
        return PrimitiveBiMaps.getIntValueOrDefault(
            directionMaps.get(direction).inverse(), packet.getClass(), -1);
    }

    /**
     * @author NovaEngineering
     * @reason Retain the nullable API; the encoder uses PacketIdAccess directly.
     */
    @Overwrite
    public Integer getPacketId(final EnumPacketDirection direction, final Packet<?> packet) {
        final int id = nova$getPacketId(direction, packet);
        return id == -1 ? null : id;
    }

    /**
     * @author NovaEngineering
     * @reason Eliminate boxing before class lookup while preserving packet construction.
     */
    @Overwrite
    public Packet<?> getPacket(final EnumPacketDirection direction, final int packetId)
        throws InstantiationException, IllegalAccessException {
        final Class<?> type =
            (Class<?>) PrimitiveBiMaps.getIntKey(directionMaps.get(direction), packetId);
        return type == null ? null : (Packet<?>) type.newInstance();
    }
}
