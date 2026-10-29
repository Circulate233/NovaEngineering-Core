package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GameRules.ValueType.class, remap = false)
public abstract class MixinGameRulesValueType {

    @Shadow
    @Final
    private static GameRules.ValueType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GameRules.ValueType[] values() {
        return $VALUES;
    }
}
