package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.scoreboard.IScoreCriteria;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = IScoreCriteria.EnumRenderType.class, remap = false)
public abstract class MixinEnumRenderType {

    @Shadow
    @Final
    private static IScoreCriteria.EnumRenderType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static IScoreCriteria.EnumRenderType[] values() {
        return $VALUES;
    }
}
