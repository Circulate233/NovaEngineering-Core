package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.tile.internal.inventory.ItemSorption;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ItemSorption.class, remap = false)
public abstract class MixinItemSorption {

    @Shadow
    @Final
    private static ItemSorption[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ItemSorption[] values() {
        return $VALUES;
    }
}
