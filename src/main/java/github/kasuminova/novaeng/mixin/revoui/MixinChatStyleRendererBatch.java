package github.kasuminova.novaeng.mixin.revoui;

import github.kasuminova.novaeng.client.gui.ChatPanelRectangleBatch;
import neofontrender.addons.chat.ChatStyleRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChatStyleRenderer.class, remap = false)
public abstract class MixinChatStyleRendererBatch {
    @Inject(method = "fadingPanel(III[FIIF)V", at = @At("HEAD"), remap = false)
    private static void nova$beginRectangleBatch(
        final int width, final int height, final int rowHeight, final float[] bottomUpFade,
        final int fill, final int border, final float minecraftOpacity, final CallbackInfo ci
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
        final int left, final int top, final int right, final int bottom, final int color
    ) {
        ChatPanelRectangleBatch.add(left, top, right, bottom, color);
    }

    @Inject(method = "fadingPanel(III[FIIF)V", at = @At("RETURN"), remap = false)
    private static void nova$flushRectangleBatch(
        final int width, final int height, final int rowHeight, final float[] bottomUpFade,
        final int fill, final int border, final float minecraftOpacity, final CallbackInfo ci
    ) {
        ChatPanelRectangleBatch.end();
    }
}
