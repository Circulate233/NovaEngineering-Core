package github.kasuminova.novaeng.mixin.enumcache.fancymenu;

import de.keksuccino.fancymenu.menu.fancy.item.items.inputfield.InputFieldCustomizationItem.InputFieldType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = InputFieldType.class, remap = false)
public abstract class MixinInputFieldType {

    @Shadow
    @Final
    private static InputFieldType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static InputFieldType[] values() {
        return $VALUES;
    }
}
