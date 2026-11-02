package github.kasuminova.novaeng.mixin.librarianlib;

import com.teamwizardry.librarianlib.features.kotlin.CommonUtilMethods;
import com.teamwizardry.librarianlib.features.kotlin.DefaultedMutableMap;
import com.teamwizardry.librarianlib.features.saving.serializers.Serializer;
import com.teamwizardry.librarianlib.features.saving.serializers.builtin.core.NamedDynamicRegistryManager.NamedDynamicRegistry;
import com.teamwizardry.librarianlib.features.saving.serializers.builtin.core.SerializeNamedDynamicFactory.SerializeDynamic;

import github.kasuminova.novaeng.common.util.NamedDynamicIdAccess;

import io.netty.buffer.ByteBuf;

import kotlin.jvm.internal.Intrinsics;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = SerializeDynamic.class, remap = false)
public abstract class MixinSerializeDynamicPrimitiveBiMap {
    @Shadow
    @Final
    private List<NamedDynamicRegistry> registry;
    @Shadow
    @Final
    private DefaultedMutableMap<Class<?>, Serializer<Object>> serializersByClass;

    @Shadow
    public abstract IllegalArgumentException notFoundError(Class<?> type);

    /**
     * @author NovaEngineering
     * @reason Resolve the first registered primitive ID without a boxed temporary list.
     */
    @Overwrite
    protected void writeBytes(final ByteBuf buf, final Object value, final boolean syncing) {
        Intrinsics.checkNotNullParameter(buf, "buf");
        Intrinsics.checkNotNullParameter(value, "value");
        for (NamedDynamicRegistry entry : registry) {
            final int id =
                ((NamedDynamicIdAccess) (Object) entry)
                    .nova$getNamedDynamicId(value.getClass());
            if (id == -1) continue;
            CommonUtilMethods.writeVarInt(buf, id);
            serializersByClass.get(value.getClass()).write(buf, value, syncing);
            return;
        }
        throw notFoundError(value.getClass());
    }
}
