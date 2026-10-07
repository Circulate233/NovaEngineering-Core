package github.kasuminova.novaeng.mixin.revoui;

import github.kasuminova.novaeng.client.gui.ChatPanelRectangleBatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "neofontrender.addons.chat.ChatStyleRenderer", remap = false)
public abstract class MixinChatStyleRendererBatch {
    @Inject(method = "fadingPanel(III[FIIF)V", at = @At("HEAD"), remap = false)
    private static void nova$beginRectangleBatch(
        final int width, final int height, final int rowHeight, final float[] rowAlphas,
        final int background, final int border, final float opacity, final CallbackInfo ci
    ) {
        ChatPanelRectangleBatch.begin();
    }

    @Redirect(
        method = "fadingPanel(III[FIIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;drawRect(IIIII)V",
            remap = true
        ),
        remap = false
    )
    private static void nova$queueRectangle(
        final int x1, final int y1, final int x2, final int y2, final int color
    ) {
        ChatPanelRectangleBatch.add(x1, y1, x2, y2, color);
    }

    @Inject(method = "fadingPanel(III[FIIF)V", at = @At("RETURN"), remap = false)
    private static void nova$flushRectangleBatch(
        final int width, final int height, final int rowHeight, final float[] rowAlphas,
        final int background, final int border, final float opacity, final CallbackInfo ci
    ) {
        ChatPanelRectangleBatch.end();
    }
}
