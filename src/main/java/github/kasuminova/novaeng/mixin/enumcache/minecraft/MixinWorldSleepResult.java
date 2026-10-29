package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = WorldProvider.WorldSleepResult.class, remap = false)
public abstract class MixinWorldSleepResult {

    @Shadow
    @Final
    private static WorldProvider.WorldSleepResult[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static WorldProvider.WorldSleepResult[] values() {
        return $VALUES;
    }
}
