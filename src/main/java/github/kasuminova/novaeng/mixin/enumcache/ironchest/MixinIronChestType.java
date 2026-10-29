package github.kasuminova.novaeng.mixin.enumcache.ironchest;

import cpw.mods.ironchest.common.blocks.chest.IronChestType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = IronChestType.class, remap = false)
public abstract class MixinIronChestType {

    @Shadow
    @Final
    private static IronChestType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static IronChestType[] values() {
        return $VALUES;
    }
}
