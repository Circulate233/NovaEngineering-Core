package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.client.sound.SoundUpdateBatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import paulscode.sound.CommandThread;
import paulscode.sound.SoundSystem;

@Mixin(value = SoundSystem.class, remap = false)
public abstract class MixinSoundSystemWakeups {
    @Redirect(method = {"setVolume", "setPitch"},
        at = @At(value = "INVOKE", target = "Lpaulscode/sound/CommandThread;interrupt()V"), require = 2)
    private void nova$deferParameterWakeup(final CommandThread thread) {
        SoundUpdateBatch.defer(thread);
    }

    @Redirect(method = "setPosition",
        at = @At(value = "INVOKE", target = "Lpaulscode/sound/CommandThread;interrupt()V"), require = 1)
    private void nova$finishParameterWakeups(final CommandThread thread) {
        SoundUpdateBatch.finish(thread);
    }
}
