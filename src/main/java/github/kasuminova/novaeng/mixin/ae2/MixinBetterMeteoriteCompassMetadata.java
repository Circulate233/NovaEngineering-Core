package github.kasuminova.novaeng.mixin.ae2;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.kasuminova.novaeng.common.performance.MeteorCompassMetadata;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

// The target accessors and callback method resolve members contributed by BetterMeteorite at runtime.
@Pseudo
@Mixin(targets = "appeng.services.CompassService", priority = 1100, remap = false)
public abstract class MixinBetterMeteoriteCompassMetadata {

    @Shadow
    @Final
    private static int CHUNK_SIZE;

    @Shadow
    @Final
    private ExecutorService executor;

    @Accessor("randomComplement$MeteoriteCompassTarget")
    public static List<ResourceLocation> nova$targets() {
        throw new AssertionError();
    }

    @Accessor("randomComplement$meta")
    public static List<Integer> nova$metadata() {
        throw new AssertionError();
    }

    @Accessor("randomComplement$constructor")
    public static Constructor<?> nova$updateConstructor() {
        throw new AssertionError();
    }

    @Dynamic("Callback merged by BetterMeteorite's MixinCompassService")
    @WrapMethod(method = "(Lnet/minecraft/world/World;IIILorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V")
    private void nova$scanArea(final World world, final int x, final int y, final int z,
                               final CallbackInfoReturnable<Future<?>> result, final Operation<Void> original)
        throws InvocationTargetException, InstantiationException, IllegalAccessException {
        if (nova$updateConstructor() == null || nova$targets() == null || nova$targets().isEmpty()) {
            original.call(world, x, y, z, result);
            return;
        }
        final int chunkX = x >> 4;
        final int band = y >> 5;
        final int chunkZ = z >> 4;
        final int lowY = band << 5;
        final int highY = lowY + 32;
        final Chunk chunk = world.getChunk(chunkX, chunkZ);
        final List<ResourceLocation> targets = nova$targets();
        final List<Integer> metadata = nova$metadata();
        for (int blockX = 0; blockX < CHUNK_SIZE; blockX++) {
            for (int blockZ = 0; blockZ < CHUNK_SIZE; blockZ++) {
                for (int blockY = lowY; blockY < highY; blockY++) {
                    final IBlockState state = chunk.getBlockState(blockX, blockY, blockZ);
                    if (MeteorCompassMetadata.matches(state, targets, metadata)) {
                        result.setReturnValue(this.executor.submit((Runnable) nova$updateConstructor()
                            .newInstance(this, world, chunkX, chunkZ, band, true)));
                        return;
                    }
                }
            }
        }
        result.setReturnValue(this.executor.submit((Runnable) nova$updateConstructor()
            .newInstance(this, world, chunkX, chunkZ, band, false)));
    }
}
