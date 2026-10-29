package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.resource.VanillaResourceType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = VanillaResourceType.class, remap = false)
public abstract class MixinVanillaResourceType {

    @Shadow
    @Final
    private static VanillaResourceType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static VanillaResourceType[] values() {
        return $VALUES;
    }
}
