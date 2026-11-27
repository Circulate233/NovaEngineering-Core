package github.kasuminova.novaeng.client.gui.widget;

import github.kasuminova.mmce.client.gui.util.AnimationValue;
import github.kasuminova.mmce.client.gui.util.MousePos;
import github.kasuminova.mmce.client.gui.util.RenderPos;
import github.kasuminova.mmce.client.gui.util.RenderSize;
import github.kasuminova.mmce.client.gui.util.TextureProperties;
import github.kasuminova.mmce.client.gui.widget.base.DynamicWidget;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import lombok.Getter;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;
import java.util.function.Function;

public class ProgressBar extends DynamicWidget {

    @Getter
    protected boolean leftToRight = true;
    @Getter
    protected boolean vertical = false;

    @Getter
    protected TextureProperties backgroundTexture = null;

    @Getter
    protected List<TextureProperties> progressTextures = new ObjectArrayList<>();

    protected AnimationValue progress = AnimationValue.ofFinished(0, 500, .25, .1, .25, 1);
    @Getter
    protected double maxProgress = 1;
    @Getter
    protected boolean shouldUseAnimation = true;

    protected Function<ProgressBar, List<String>> tooltipFunction = null;

    @Override
    public void render(final WidgetGui widgetGui, final RenderSize renderSize, final RenderPos renderPos, final MousePos mousePos) {
        GuiScreen gui = widgetGui.getGui();

        // Background.
        if (backgroundTexture != null) {
            backgroundTexture.render(renderPos, gui);
        }

        // ProgressBar / Foreground.
        if (progressTextures.isEmpty() || maxProgress <= 0) {
            return;
        }

        double percent = Math.min(1, progress.get() / maxProgress);

        // Progress background
        final TextureProperties background = findProgressBackgroundTex(percent);
        if (background != null) background.render(renderPos, gui);

        float renderPercent = (float) percent;
        final TextureProperties foreground = findProgressForegroundTex(percent);
        if (foreground == null) return;
        foreground.bind(gui.mc.getTextureManager());
        if (isHorizontal()) {
            if (leftToRight) {
                gui.drawTexturedModalRect(
                    renderPos.posX(), renderPos.posY(),
                    foreground.texX(), foreground.texY(),
                    (int) (foreground.width() * renderPercent), foreground.height()
                );
            } else {
                gui.drawTexturedModalRect(
                    renderPos.posX() + (int) (foreground.width() * (1 - renderPercent)), renderPos.posY(),
                    foreground.texX(), foreground.texY(),
                    (int) (foreground.width() * renderPercent), foreground.height()
                );
            }
        } else {
            if (leftToRight) {
                gui.drawTexturedModalRect(
                    renderPos.posX(), renderPos.posY(),
                    foreground.texX(), foreground.texY(),
                    foreground.width(), (int) (foreground.height() * renderPercent)
                );
            } else {
                gui.drawTexturedModalRect(
                    renderPos.posX(), renderPos.posY() + (int) (foreground.height() * (1 - renderPercent)),
                    foreground.texX(), foreground.texY() + (int) (foreground.height() * (1 - renderPercent)),
                    foreground.width(), (int) (foreground.height() * renderPercent)
                );
            }
        }
    }

    protected TextureProperties findProgressForegroundTex(final double percent) {
        if (progressTextures.isEmpty()) {
            return null;
        }

        int idx = (int) percent;
        if (idx >= progressTextures.size()) {
            return progressTextures.getLast();
        } else {
            return progressTextures.get(Math.max(idx, 0));
        }
    }

    protected TextureProperties findProgressBackgroundTex(final double percent) {
        if (progressTextures.isEmpty()) {
            return null;
        }

        int idx = ((int) percent) - 1;
        if (idx <= 0) {
            return null;
        } else if (idx >= progressTextures.size()) {
            return null;
        }
        return progressTextures.get(idx);
    }

    // Progress settings.

    public double getProgressPercent() {
        if (maxProgress <= 0) {
            return 0;
        }
        return progress.get() / maxProgress;
    }

    public double getProgress() {
        return progress.get();
    }

    public ProgressBar setProgress(final double progress) {
        if (shouldUseAnimation) {
            this.progress.set(progress);
        } else {
            this.progress.setImmediate(progress);
        }
        return this;
    }

    public ProgressBar setMaxProgress(final float maxProgress) {
        this.maxProgress = maxProgress;
        if (progress.getTargetValue() > maxProgress) {
            if (shouldUseAnimation) {
                progress.set(maxProgress);
            } else {
                progress.setImmediate(maxProgress);
            }
        }
        return this;
    }

    public ProgressBar setShouldUseAnimation(final boolean shouldUseAnimation) {
        this.shouldUseAnimation = shouldUseAnimation;
        if (!shouldUseAnimation && !progress.isAnimFinished()) {
            progress.setImmediate(progress.getTargetValue());
        }
        return this;
    }

    // Vertical & Horizontal settings.

    public ProgressBar setVertical(final boolean vertical) {
        this.vertical = vertical;
        return this;
    }

    public boolean isHorizontal() {
        return !vertical;
    }

    // Left to right & Up to down settings.

    public ProgressBar setHorizontal(final boolean horizontal) {
        this.vertical = !horizontal;
        return this;
    }

    public ProgressBar setLeftToRight(final boolean leftToRight) {
        this.leftToRight = leftToRight;
        return setHorizontal(true);
    }

    public ProgressBar setRightToLeft() {
        return setLeftToRight(!leftToRight);
    }

    public boolean isRightToLeft() {
        return !leftToRight;
    }

    public boolean isUpToDown() {
        return !leftToRight;
    }

    public ProgressBar setUpToDown(final boolean upToDown) {
        this.leftToRight = upToDown;
        return setVertical(true);
    }

    public boolean isDownToUp() {
        return leftToRight;
    }

    public ProgressBar setDownToUp(final boolean downToUp) {
        return setUpToDown(!downToUp);
    }

    // Background & Foreground settings.

    public ProgressBar setBackgroundTexture(final TextureProperties backgroundTexture) {
        this.backgroundTexture = backgroundTexture;
        return this;
    }

    public ProgressBar addForegroundTexture(final TextureProperties foregroundTex) {
        progressTextures.add(foregroundTex);
        return this;
    }

    // Tooltips

    @Override
    public List<String> getHoverTooltips(final WidgetGui widgetGui, final MousePos mousePos) {
        return tooltipFunction != null ? tooltipFunction.apply(this) : ObjectLists.emptyList();
    }

    public ProgressBar setTooltipFunction(final Function<ProgressBar, List<String>> tooltipFunction) {
        this.tooltipFunction = tooltipFunction;
        return this;
    }

}
