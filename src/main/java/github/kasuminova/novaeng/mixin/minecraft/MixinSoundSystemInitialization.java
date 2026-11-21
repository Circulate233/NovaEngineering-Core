package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.InitializationWait;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;

@Mixin(value = SoundSystem.class, remap = false)
public abstract class MixinSoundSystemInitialization {
    @Shadow private static boolean initialized;

    @Redirect(method = "newLibrary", at = @At(value = "INVOKE", target = "Lpaulscode/sound/SoundSystem;snooze(J)V"), require = 1)
    private void nova$awaitInitialization(final long millis) {
        InitializationWait.await(SoundSystemConfig.THREAD_SYNC, () -> initialized, millis);
    }

    @Redirect(method = "initialized(ZZ)Z", at = @At(value = "FIELD",
        target = "Lpaulscode/sound/SoundSystem;initialized:Z", opcode = Opcodes.PUTSTATIC), require = 1)
    private static void nova$signalInitialization(final boolean value) {
        // The original setter already holds THREAD_SYNC. Reads keep their original allocation-free path.
        initialized = value;
        if (value) SoundSystemConfig.THREAD_SYNC.notifyAll();
    }
}
