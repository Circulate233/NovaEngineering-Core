package github.kasuminova.novaeng.mixin.enumcache.reborncore;

import org.checkerframework.framework.qual.LiteralKind;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = LiteralKind.class, remap = false)
public abstract class MixinLiteralKind {

    @Shadow
    @Final
    private static LiteralKind[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static LiteralKind[] values() {
        return $VALUES;
    }
}
