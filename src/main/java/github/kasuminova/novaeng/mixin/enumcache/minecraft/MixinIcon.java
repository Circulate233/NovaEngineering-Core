package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.gui.GuiLockIconButton;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GuiLockIconButton.Icon.class, remap = false)
public abstract class MixinIcon {

    @Shadow
    @Final
    private static GuiLockIconButton.Icon[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GuiLockIconButton.Icon[] values() {
        return $VALUES;
    }
}
