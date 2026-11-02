package github.kasuminova.novaeng.mixin.ae2;

import appeng.core.AELog;
import appeng.hooks.TickHandler;
import appeng.util.IWorldCallable;
import com.google.common.base.Stopwatch;
import github.kasuminova.novaeng.common.performance.MeteorWriteBatch;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

@Mixin(value = TickHandler.class, remap = false)
public abstract class MixinMeteorWriteBatch {
    /**
     * @author NovaEngineering
     * @reason Coalesce meteor region writes within the existing queue budget and flush on all exits.
     */
    @Overwrite
    private void processQueue(final Queue<IWorldCallable<?>> queue, final World world) {
        if (queue == null) return;
        final Stopwatch stopwatch = Stopwatch.createStarted();
        try (final MeteorWriteBatch batch = MeteorWriteBatch.open()) {
            IWorldCallable<?> callable;
            while ((callable = queue.poll()) != null) {
                try {
                    callable.call(world);
                    if (stopwatch.elapsed(TimeUnit.MILLISECONDS) > 50L) break;
                } catch (final Exception failure) {
                    AELog.debug(failure);
                }
            }
        }
    }
}
