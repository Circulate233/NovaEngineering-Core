package github.kasuminova.novaeng.mixin.bettermeteor;

import appeng.services.CompassService;

import com.circulation.better_meteorite.BMConfig;
import com.circulation.better_meteorite.util.Function;

import github.kasuminova.novaeng.common.performance.MeteorCompassMetadata;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Runs before BetterMeteorite's HEAD callback, retaining its configured target semantics.
 */
@Mixin(value = CompassService.class, remap = false, priority = 2000)
public abstract class MixinCompassTargetScan {
    @Shadow
    @Final
    private static int CHUNK_SIZE;
    @Unique
    private static volatile MeteorCompassMetadata nova$targets;
    @Unique
    private static MethodHandle nova$updatePostConstructor;
    @Shadow
    @Final
    private ExecutorService executor;

    @Unique
    private static synchronized MeteorCompassMetadata nova$initializeTargets()
        throws ReflectiveOperationException {
        if (nova$targets != null) return nova$targets;
        final var lookup =
            MethodHandles.privateLookupIn(CompassService.class, MethodHandles.lookup());
        final Class<?> type = lookup.findClass("appeng.services.CompassService$CMUpdatePost");
        nova$updatePostConstructor =
            MethodHandles.privateLookupIn(type, lookup)
                         .findConstructor(
                             type,
                             MethodType.methodType(
                                 void.class,
                                 CompassService.class,
                                 World.class,
                                 int.class,
                                 int.class,
                                 int.class,
                                 boolean.class))
                         .asType(
                             MethodType.methodType(
                                 Runnable.class,
                                 CompassService.class,
                                 World.class,
                                 int.class,
                                 int.class,
                                 int.class,
                                 boolean.class));
        final ObjectArrayList<IBlockState> states = new ObjectArrayList<>();
        for (final String target : BMConfig.MeteoriteCompassTarget) {
            final IBlockState state = Function.getBlockFromName(target);
            if (state != null) states.add(state);
        }
        final MeteorCompassMetadata matcher = new MeteorCompassMetadata(states);
        nova$targets = matcher;
        return matcher;
    }

    @Inject(
        method = "updateArea(Lnet/minecraft/world/World;III)Ljava/util/concurrent/Future;",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void nova$scanConfiguredTargets(
        final World world,
        final int x,
        final int y,
        final int z,
        final CallbackInfoReturnable<Future<?>> result)
        throws Throwable {
        final int chunkX = x >> 4;
        final int chunkZ = z >> 4;
        final int band = y >> 5;
        final Chunk chunk = world.getChunk(chunkX, chunkZ);
        MeteorCompassMetadata targets = nova$targets;
        if (targets == null) targets = nova$initializeTargets();
        final MeteorCompassMetadata.Scan scan = targets.scan();
        boolean found = false;
        if (!targets.isEmpty()) {
            search:
            for (int blockX = 0; blockX < CHUNK_SIZE; blockX++) {
                for (int blockZ = 0; blockZ < CHUNK_SIZE; blockZ++) {
                    for (int blockY = band << 5, endY = blockY + 32; blockY < endY; blockY++) {
                        if (scan.matches(chunk.getBlockState(blockX, blockY, blockZ))) {
                            found = true;
                            break search;
                        }
                    }
                }
            }
        }
        final Runnable update =
            (Runnable)
                nova$updatePostConstructor.invokeExact(
                    (CompassService) (Object) this, world, chunkX, chunkZ, band, found);
        result.setReturnValue(executor.submit(update));
    }
}
