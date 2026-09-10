package github.kasuminova.novaeng.mixin.obscuretooltips;

import dev.obscuria.tooltips.client.TooltipRenderer;
import dev.obscuria.tooltips.client.component.TooltipComponent;
import dev.obscuria.tooltips.client.render.GuiGraphics;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = TooltipRenderer.class, remap = false)
public class MixinTooltipRenderer {

    @Inject(method = "render", at = @At("HEAD"))
    private static void novaeng$beginRender(final GuiGraphics graphics, final FontRenderer font,
                                            final List<String> rawLines, final List<TooltipComponent> components,
                                            final int mouseX, final int mouseY, final int screenWidth,
                                            final int screenHeight, final ItemStack stack, final CallbackInfo ci) {
        ObscureTooltipGeometry.beginRender(rawLines);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", ordinal = 0,
        target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z"))
    private static boolean novaeng$remapPostBackground(final EventBus bus, final Event event) {
        return bus.post(ObscureTooltipGeometry.remapPostEvent(event));
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", ordinal = 1,
        target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z"))
    private static boolean novaeng$remapPostText(final EventBus bus, final Event event) {
        return bus.post(ObscureTooltipGeometry.remapPostEvent(event));
    }
}
