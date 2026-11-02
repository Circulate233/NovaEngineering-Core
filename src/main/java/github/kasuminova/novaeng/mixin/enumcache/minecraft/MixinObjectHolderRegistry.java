package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.registries.ObjectHolderRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ObjectHolderRegistry.class, remap = false)
public abstract class MixinObjectHolderRegistry {

    @Shadow
    @Final
    private static ObjectHolderRegistry[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ObjectHolderRegistry[] values() {
        return $VALUES;
    }
}
