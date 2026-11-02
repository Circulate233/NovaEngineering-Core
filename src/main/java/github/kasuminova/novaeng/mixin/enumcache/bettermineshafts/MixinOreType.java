package github.kasuminova.novaeng.mixin.enumcache.bettermineshafts;

import com.yungnickyoung.minecraft.bettermineshafts.world.generator.pieces.OreDeposit.OreType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = OreType.class, remap = false)
public abstract class MixinOreType {

    @Shadow
    @Final
    private static OreType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static OreType[] values() {
        return $VALUES;
    }
}
