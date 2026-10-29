package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.entity.ai.EntityMoveHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EntityMoveHelper.Action.class, remap = false)
public abstract class MixinEntityMoveHelperAction {

    @Shadow
    @Final
    private static EntityMoveHelper.Action[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EntityMoveHelper.Action[] values() {
        return $VALUES;
    }
}
