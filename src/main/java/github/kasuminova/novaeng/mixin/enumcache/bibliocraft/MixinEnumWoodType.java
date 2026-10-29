package github.kasuminova.novaeng.mixin.enumcache.bibliocraft;

import jds.bibliocraft.blocks.BiblioWoodBlock.EnumWoodType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumWoodType.class, remap = false)
public abstract class MixinEnumWoodType {

    @Shadow
    @Final
    private static EnumWoodType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumWoodType[] values() {
        return $VALUES;
    }
}
