package github.kasuminova.novaeng.mixin.minecraft;

import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.novaeng.common.performance.ObserverBlockMethods;
import github.kasuminova.novaeng.common.performance.ObserverPalette;
import net.minecraft.block.state.IBlockState;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.BlockStatePaletteLinear;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockStatePaletteLinear.class)
public abstract class MixinObserverPaletteLinear implements ObserverPalette {
    @Shadow
    private int arraySize;
    @Unique
    private boolean nova$mayHaveObserver;

    @Redirect(method = "idFor", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
        target = "Lnet/minecraft/world/chunk/BlockStatePaletteLinear;arraySize:I"), require = 1)
    private void nova$classifyInsertedState(final BlockStatePaletteLinear palette, final int newSize,
                                            @Local(argsOnly = true) final IBlockState state) {
        if (!nova$mayHaveObserver) nova$mayHaveObserver = !ObserverBlockMethods.isEmpty(state);
        arraySize = newSize;
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
