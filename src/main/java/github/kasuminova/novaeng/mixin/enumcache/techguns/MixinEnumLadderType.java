package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.blocks.EnumLadderType;

@Mixin(value = EnumLadderType.class, remap = false)
public abstract class MixinEnumLadderType {

    @Shadow
    @Final
    private static EnumLadderType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumLadderType[] values() {
        return $VALUES;
    }
}
