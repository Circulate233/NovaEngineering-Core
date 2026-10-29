package github.kasuminova.novaeng.mixin.enumcache.jecharacters;

import me.towdium.jecharacters.JechConfig.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Item.class, remap = false)
public abstract class MixinItem {

    @Shadow
    @Final
    private static Item[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Item[] values() {
        return $VALUES;
    }
}
