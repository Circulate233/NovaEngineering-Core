package github.kasuminova.novaeng.common.util;

import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;

public interface PacketIdAccess {
    /**
     * Returns -1 when the packet class is unregistered.
     */
    int nova$getPacketId(EnumPacketDirection direction, Packet<?> packet);
}
