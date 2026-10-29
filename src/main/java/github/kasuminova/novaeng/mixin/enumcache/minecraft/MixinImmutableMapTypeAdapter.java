package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.common.util.JsonUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = JsonUtils.ImmutableMapTypeAdapter.class, remap = false)
public abstract class MixinImmutableMapTypeAdapter {

    @Shadow
    @Final
    private static JsonUtils.ImmutableMapTypeAdapter[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static JsonUtils.ImmutableMapTypeAdapter[] values() {
        return $VALUES;
    }
}
