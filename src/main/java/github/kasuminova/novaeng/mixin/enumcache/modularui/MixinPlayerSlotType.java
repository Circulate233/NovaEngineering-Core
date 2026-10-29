package github.kasuminova.novaeng.mixin.enumcache.modularui;

import com.cleanroommc.modularui.widgets.slot.PlayerSlotType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PlayerSlotType.class, remap = false)
public abstract class MixinPlayerSlotType {

    @Shadow
    @Final
    private static PlayerSlotType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PlayerSlotType[] values() {
        return $VALUES;
    }
}
