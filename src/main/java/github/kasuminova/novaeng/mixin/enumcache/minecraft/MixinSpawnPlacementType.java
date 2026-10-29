package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.entity.EntityLiving;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EntityLiving.SpawnPlacementType.class, remap = false)
public abstract class MixinSpawnPlacementType {

    @Shadow
    @Final
    private static EntityLiving.SpawnPlacementType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EntityLiving.SpawnPlacementType[] values() {
        return $VALUES;
    }
}
