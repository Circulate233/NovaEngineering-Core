package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GameType.class, remap = false)
public abstract class MixinGameType {

    @Shadow
    @Final
    private static GameType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GameType[] values() {
        return $VALUES;
    }
}
