package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.machines.machine.spawner.SpawnerNotification;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SpawnerNotification.class, remap = false)
public abstract class MixinSpawnerNotification {

    @Shadow
    @Final
    private static SpawnerNotification[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SpawnerNotification[] values() {
        return $VALUES;
    }
}
