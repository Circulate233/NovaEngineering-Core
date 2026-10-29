package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.end.DragonSpawnManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DragonSpawnManager.class, remap = false)
public abstract class MixinDragonSpawnManager {

    @Shadow
    @Final
    private static DragonSpawnManager[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DragonSpawnManager[] values() {
        return $VALUES;
    }
}
