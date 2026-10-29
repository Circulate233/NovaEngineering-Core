package github.kasuminova.novaeng.mixin.enumcache.tconstruct;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.shared.block.BlockSlime.SlimeType;

@Mixin(value = SlimeType.class, remap = false)
public abstract class MixinSlimeType {

    @Shadow
    @Final
    private static SlimeType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SlimeType[] values() {
        return $VALUES;
    }
}
