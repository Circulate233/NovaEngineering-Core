package github.kasuminova.novaeng.mixin.enumcache.ironchest;

import cpw.mods.ironchest.common.blocks.shulker.IronShulkerBoxType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = IronShulkerBoxType.class, remap = false)
public abstract class MixinIronShulkerBoxType {

    @Shadow
    @Final
    private static IronShulkerBoxType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static IronShulkerBoxType[] values() {
        return $VALUES;
    }
}
