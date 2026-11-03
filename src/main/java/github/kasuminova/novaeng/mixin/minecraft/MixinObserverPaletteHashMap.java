package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.ObserverBlockMethods;
import github.kasuminova.novaeng.common.performance.ObserverPalette;
import net.minecraft.block.state.IBlockState;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IntIdentityHashBiMap;
import net.minecraft.world.chunk.BlockStatePaletteHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockStatePaletteHashMap.class)
public abstract class MixinObserverPaletteHashMap implements ObserverPalette {
    @Unique
    private boolean nova$mayHaveObserver;

    @Redirect(method = "idFor", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/util/IntIdentityHashBiMap;add(Ljava/lang/Object;)I"), require = 1)
    private int nova$classifyInsertedState(final IntIdentityHashBiMap<IBlockState> palette, final Object value) {
        final IBlockState state = (IBlockState) value;
        if (!nova$mayHaveObserver) nova$mayHaveObserver = !ObserverBlockMethods.isEmpty(state);
        return palette.add(state);
    }

    // The network reader is stripped on a dedicated server.
    @Inject(method = "read", at = @At("HEAD"), require = 0)
    private void nova$invalidateRead(final PacketBuffer buffer, final CallbackInfo ci) {
        nova$mayHaveObserver = true;
    }

    @Override
    public boolean nova$mayHaveObserver() {
        return nova$mayHaveObserver;
    }
}
