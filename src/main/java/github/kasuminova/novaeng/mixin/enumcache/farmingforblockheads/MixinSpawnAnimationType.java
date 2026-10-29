package github.kasuminova.novaeng.mixin.enumcache.farmingforblockheads;

import net.blay09.mods.farmingforblockheads.entity.EntityMerchant.SpawnAnimationType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SpawnAnimationType.class, remap = false)
public abstract class MixinSpawnAnimationType {

    @Shadow
    @Final
    private static SpawnAnimationType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SpawnAnimationType[] values() {
        return $VALUES;
    }
}
