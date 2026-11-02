package github.kasuminova.novaeng.mixin.enumcache.ftbquests;

import com.feed_the_beast.ftbquests.block.BlockDetector.Variant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Variant.class, remap = false)
public abstract class MixinVariant {

    @Shadow
    @Final
    private static Variant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Variant[] values() {
        return $VALUES;
    }
}
