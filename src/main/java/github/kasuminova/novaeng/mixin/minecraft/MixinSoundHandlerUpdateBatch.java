package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.client.sound.SoundUpdateBatch;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SoundHandler.class)
public abstract class MixinSoundHandlerUpdateBatch {
    @Redirect(method = "update", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/audio/SoundManager;updateAllSounds()V"), require = 1)
    private void nova$batchSoundParameterWakeups(final SoundManager manager) {
        SoundUpdateBatch.begin();
        try {
            manager.updateAllSounds();
        } finally {
            SoundUpdateBatch.end();
        }
    }
}
