package github.kasuminova.novaeng.mixin.nuclearcraft;

import github.kasuminova.novaeng.common.performance.ProcessorDirections;
import nc.tile.processor.IProcessor;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(value = IProcessor.class, remap = false)
public interface MixinProcessorAutoPushDirections {
    /**
     * @author NovaEngineering
     * @reason Avoid rebuilding the same direction array on every accelerated processor tick.
     */
    @Overwrite
    default boolean autoPush() {
        final IProcessor<?, ?, ?> processor = (IProcessor<?, ?, ?>) this;
        final IProcessor.HandlerPair[] handlers = processor.getAdjacentHandlers();
        if (handlers == null) return false;
        final List<EnumFacing> directions = ProcessorDirections.snapshot(handlers);
        // Keep virtual dispatch, fresh capability/inventory/tank reads and the original tick rotation.
        return processor.autoPushInternal(handlers, processor.getInventoryStacks(), processor.getTanks(),
            directions, directions.size(), (int) processor.getTileWorld().getTotalWorldTime());
    }
}
