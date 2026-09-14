package github.kasuminova.novaeng.client.gui.widget.vanilla;

import ae2.client.gui.widgets.ITooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButtonImage;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.jetbrains.annotations.NotNull;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GuiButtonImageExt extends GuiButtonImage implements ITooltip {

    private String message = "";

    public GuiButtonImageExt(final int buttonId, final int xIn, final int yIn, final int widthIn, final int heightIn, final int textureOffestX, final int textureOffestY, final int p_i47392_8_, final ResourceLocation resource) {
        super(buttonId, xIn, yIn, widthIn, heightIn, textureOffestX, textureOffestY, p_i47392_8_, resource);
    }

    @Override
    public void drawButton(final @NotNull Minecraft mc, final int mouseX, final int mouseY, final float partialTicks) {
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1f, 1f, 1f, 1f);
        super.drawButton(mc, mouseX, mouseY, partialTicks);
    }

    @Override
    public @NotNull List<ITextComponent> getTooltipMessage() {
        if (message.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new TextComponentString(message));
    }

    public GuiButtonImageExt setMessage(final String message) {
        this.message = Objects.requireNonNull(message, "message");
        return this;
    }

    @Override
    public Rectangle getTooltipArea() {
        return new Rectangle(x, y, width, height);
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }

}
