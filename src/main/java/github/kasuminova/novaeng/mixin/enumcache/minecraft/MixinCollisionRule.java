package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Team.CollisionRule.class, remap = false)
public abstract class MixinCollisionRule {

    @Shadow
    @Final
    private static Team.CollisionRule[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Team.CollisionRule[] values() {
        return $VALUES;
    }
}
