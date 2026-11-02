package github.kasuminova.novaeng.mixin.architecturecraft;

import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.common.ConcreteLog;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.network.Message;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.network.NetworkContext;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.network.WireField;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.network.annotation.field.Optional;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.network.exception.BadMessageException;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.network.exception.WrongSideException;
import com.google.common.collect.BiMap;
import com.google.common.collect.Multiset;

import github.kasuminova.novaeng.common.util.collection.Reference2IntBiMap;
import github.kasuminova.novaeng.common.util.collection.Reference2IntHashBiMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(value = NetworkContext.class, remap = false)
public abstract class MixinNetworkContextPrimitiveBiMap {
    @Shadow
    @Final
    @Mutable
    protected BiMap<Class<? extends Message>, Integer> packetIds;
    @Shadow
    @Final
    protected Map<Class<? extends Message>, List<WireField<?>>> marshallers;
    @Shadow
    @Final
    protected Multiset<Class<? extends Message>> booleanCount;
    @Shadow
    @Final
    protected Multiset<Class<? extends Message>> optionalCount;
    @Shadow
    @Final
    protected String channel;
    @Shadow
    private int nextPacketId;

    @Redirect(
        method = {"<init>"},
        at =
        @At(
            value = "FIELD",
            target =
                "Lcom/elytradev/architecturecraft/repackage/com/elytradev/concrete/network/NetworkContext;packetIds:Lcom/google/common/collect/BiMap;",
            opcode = Opcodes.PUTFIELD),
        require = 1)
    private void nova$primitiveTable(
        final NetworkContext owner, final BiMap<Class<? extends Message>, Integer> original) {
        final Reference2IntHashBiMap<Class<? extends Message>> replacement =
            new Reference2IntHashBiMap<>(16);
        replacement.putAll(original);
        packetIds = replacement;
    }

    @Shadow
    private Message instantiateMessage(Class<? extends Message> clazz) {
        throw new AssertionError();
    }

    /**
     * @author NovaEngineering
     * @reason Use primitive packet IDs throughout registration and packet encoding or decoding.
     */
    @Overwrite
    public NetworkContext register(Class<? extends Message> clazz) {
        if (this.packetIds.containsKey(clazz)) {
            ConcreteLog.warn("{} was registered twice", clazz);
            return (NetworkContext) (Object) this;
        }
        ((Reference2IntBiMap<Class<? extends Message>>) packetIds)
            .putDiscard(clazz, nextPacketId++, false);
        List<WireField<?>> fields = new ObjectArrayList<>();
        for (Class<?> cursor = clazz;
             cursor != null && cursor != Object.class;
             cursor = cursor.getSuperclass()) {
            for (Field f : cursor.getDeclaredFields()) {
                if (Modifier.isTransient(f.getModifiers()) || Modifier.isStatic(f.getModifiers()))
                    continue;
                if (f.getType() == Boolean.TYPE) {
                    this.booleanCount.add(clazz);
                }
                if (f.getAnnotation(Optional.class) != null) {
                    this.optionalCount.add(clazz);
                }
                WireField<?> wf = new WireField<>(f);
                fields.add(wf);
            }
        }
        this.marshallers.put(clazz, fields);
        return (NetworkContext) (Object) this;
    }

    /**
     * @author NovaEngineering
     * @reason Use primitive packet IDs throughout registration and packet encoding or decoding.
     */
    @Overwrite
    protected FMLProxyPacket getPacketFrom(Message m) {
        if (!this.packetIds.containsKey(m.getClass())) {
            throw new BadMessageException(m.getClass() + " is not registered");
        }
        PacketBuffer payload = new PacketBuffer(Unpooled.buffer());
        payload.writeByte(((Reference2IntBiMap<?>) packetIds).getIntRequired(m.getClass()));
        int bools = this.booleanCount.count(m.getClass()) + this.optionalCount.count(m.getClass());
        if (bools > 0) {
            BooleanList li = new BooleanArrayList(bools);
            for (WireField<?> wf : this.marshallers.get(m.getClass())) {
                if (wf.getType() == Boolean.TYPE) {
                    li.add((boolean) wf.get(m));
                    continue;
                }
                if (!wf.isOptional()) continue;
                li.add(wf.get(m) != null);
            }
            for (int i = 0; i < (bools + 7) / 8; ++i) {
                int by = 0;
                for (int j = i * 8; j < Math.min(li.size(), i + 8); ++j) {
                    if (!li.getBoolean(j)) continue;
                    by |= 1 << j;
                }
                payload.writeByte(by);
            }
        }
        for (WireField<?> field : this.marshallers.get(m.getClass())) {
            if (field.getType() != Boolean.TYPE) field.marshal(m, payload);
        }
        return new FMLProxyPacket(payload, this.channel);
    }

    @SuppressWarnings("unchecked")
    @Unique
    private static void nova$setBooleanField(final WireField<?> field, final Message message,
                                             final boolean value) {
        ((WireField<Boolean>) field).set(message, value);
    }

    /**
     * @author NovaEngineering
     * @reason Use primitive packet IDs throughout registration and packet encoding or decoding.
     */
    @Overwrite
    private Message readPacket(Side side, ByteBuf payload) {
        Message m;
        short id = payload.readUnsignedByte();
        if (!((Reference2IntBiMap<?>) packetIds).containsValue(id)) {
            throw new IllegalArgumentException("Unknown packet id " + id);
        }
        Class<? extends Message> clazz =
            ((Reference2IntBiMap<Class<? extends Message>>) packetIds).inverse().get(id);
        try {
            m = this.instantiateMessage(clazz);
        } catch (Throwable t) {
            throw new BadMessageException("Cannot instanciate message class " + clazz, t);
        }
        if (((InvokerMessageSide) m).nova$getMessageSide() != side) {
            throw new WrongSideException(
                "Cannot receive packet of type " + clazz + " on side " + side);
        }
        Set<WireField<?>> present =
            new ObjectOpenHashSet<>(this.marshallers.get(m.getClass()).size());
        int bools = this.booleanCount.count(m.getClass()) + this.optionalCount.count(m.getClass());
        if (bools > 0) {
            List<WireField<?>> flagFields = new ObjectArrayList<>(bools);
            for (WireField<?> wf : this.marshallers.get(m.getClass())) {
                if (wf.getType() == Boolean.TYPE) {
                    flagFields.add(wf);
                    present.add(wf);
                    continue;
                }
                if (wf.isOptional()) {
                    flagFields.add(wf);
                } else {
                    present.add(wf);
                }
            }
            for (int i = 0; i < (bools + 7) / 8; ++i) {
                short by = payload.readUnsignedByte();
                for (int j = i * 8; j < Math.min(flagFields.size(), i + 8); ++j) {
                    boolean val = (by & 1 << j - i) != 0;
                    final WireField<?> field = flagFields.get(j);
                    if (field.getType() == Boolean.TYPE) {
                        nova$setBooleanField(field, m, val);
                    } else if (val) {
                        present.add(field);
                    }
                }
            }
        } else {
            present.addAll(this.marshallers.get(m.getClass()));
        }
        for (WireField<?> field : this.marshallers.get(m.getClass())) {
            if (field.getType() != Boolean.TYPE && present.contains(field)) field.unmarshal(m, payload);
        }
        return m;
    }
}
