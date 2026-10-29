package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.gui.advancements.AdvancementState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AdvancementState.class, remap = false)
public abstract class MixinAdvancementState {

    @Shadow
    @Final
    private static AdvancementState[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AdvancementState[] values() {
        return $VALUES;
    }
}
