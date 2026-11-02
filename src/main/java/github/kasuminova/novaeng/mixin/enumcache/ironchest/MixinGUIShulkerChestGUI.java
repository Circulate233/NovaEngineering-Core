package github.kasuminova.novaeng.mixin.enumcache.ironchest;

import cpw.mods.ironchest.client.gui.shulker.GUIShulkerChest.GUI;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GUI.class, remap = false)
public abstract class MixinGUIShulkerChestGUI {

    @Shadow
    @Final
    private static GUI[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GUI[] values() {
        return $VALUES;
    }
}
