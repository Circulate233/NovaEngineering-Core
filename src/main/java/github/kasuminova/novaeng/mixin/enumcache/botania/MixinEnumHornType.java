package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.item.IHornHarvestable.EnumHornType;

@Mixin(value = EnumHornType.class, remap = false)
public abstract class MixinEnumHornType {

    @Shadow
    @Final
    private static EnumHornType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumHornType[] values() {
        return $VALUES;
    }
}
