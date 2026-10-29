package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.invpanel.client.SortOrder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SortOrder.class, remap = false)
public abstract class MixinSortOrder {

    @Shadow
    @Final
    private static SortOrder[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SortOrder[] values() {
        return $VALUES;
    }
}
