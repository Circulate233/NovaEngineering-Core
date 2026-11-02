package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.ServerWorldEventHandler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(World.class)
public abstract class MixinWorldEmptyLightListener {
    @Shadow
    @Final
    protected List<IWorldEventListener> eventListeners;

    /**
     * @author NovaEngineering
     * @reason Skip the exact server listener's empty method without allocating a cancellable injection callback.
     */
    @Overwrite
    public void notifyLightSet(final BlockPos pos) {
        if (eventListeners.size() == 1 && eventListeners.get(0).getClass() == ServerWorldEventHandler.class) return;
        for (int i = 0; i < eventListeners.size(); i++) eventListeners.get(i).notifyLightSet(pos);
    }
}
