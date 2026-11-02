package github.kasuminova.novaeng.mixin.librarianlib;

import com.teamwizardry.librarianlib.features.network.PacketBase;
import com.teamwizardry.librarianlib.features.network.PacketHandler;
import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.common.util.collection.Object2IntBiMap;
import github.kasuminova.novaeng.common.util.collection.Object2IntHashBiMap;

import kotlin.jvm.internal.Intrinsics;

import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = PacketHandler.class, remap = false)
public abstract class MixinPacketHandlerPrimitiveBiMap {
    @Unique
    private static final Object2IntBiMap<Class<?>> nova$discriminators =
        new Object2IntHashBiMap<>();

    @Shadow
    @Final
    public static SimpleNetworkWrapper NETWORK;
    @Shadow
    private static int id;

    /**
     * @author NovaEngineering
     * @reason Register primitive packet IDs and apply the startup field policy in the same registration path.
     */
    @Overwrite
    public static <T extends PacketBase> void register(
        final Class<T> clazz, final Side targetSide) {
        Intrinsics.checkNotNullParameter(clazz, "clazz");
        Intrinsics.checkNotNullParameter(targetSide, "targetSide");
        // First serialization initializes fields when startup optimization is enabled.
        if (!NovaEngCoreConfig.PERFORMANCE.optimizeStartupWork) {
            AbstractSaveHandler.cacheFields(clazz);
        }
        nova$discriminators.putDiscard(clazz, id, false);
        NETWORK.registerMessage(new PacketHandler.Handler<>(), clazz, id, targetSide);
        id++;
    }

    /**
     * @author NovaEngineering
     * @reason Read the primitive packet discriminator.
     */
    @Overwrite
    public final int getDiscriminator(final Class<?> clazz) {
        Intrinsics.checkNotNullParameter(clazz, "clazz");
        return nova$discriminators.getIntOrDefault(clazz, -1);
    }

    /**
     * @author NovaEngineering
     * @reason Resolve packet classes without boxing their ID.
     */
    @Overwrite
    public final @Nullable Class<?> getPacketClass(final int descriminator) {
        return nova$discriminators.inverse().get(descriminator);
    }
}
