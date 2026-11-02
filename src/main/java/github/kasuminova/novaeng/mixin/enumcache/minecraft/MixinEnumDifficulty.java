package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.EnumDifficulty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumDifficulty.class, remap = false)
public abstract class MixinEnumDifficulty {

    @Shadow
    @Final
    private static EnumDifficulty[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumDifficulty[] values() {
        return $VALUES;
    }
}
