package github.kasuminova.novaeng.mixin.ae2;

import appeng.client.gui.implementations.GuiCraftConfirm;
import net.minecraft.client.gui.GuiButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GuiCraftConfirm.class, remap = false)
public interface AccessorGuiCraftConfirm {

    @Accessor("cancel")
    GuiButton n$getCancel();

    @Accessor("start")
    GuiButton n$getStart();
}
