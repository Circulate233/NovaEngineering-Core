package github.kasuminova.novaeng.mixin.enumcache.ancientspellcraft;

import com.windanesz.ancientspellcraft.item.ItemRelic.RelicType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RelicType.class, remap = false)
public abstract class MixinRelicType {

    @Shadow
    @Final
    private static RelicType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RelicType[] values() {
        return $VALUES;
    }
}
