package github.kasuminova.mmce.client.gui.widget;

import github.kasuminova.mmce.client.gui.util.MousePos;
import github.kasuminova.mmce.client.gui.util.RenderPos;
import github.kasuminova.mmce.client.gui.util.RenderSize;
import github.kasuminova.mmce.client.gui.widget.base.DynamicWidget;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import github.kasuminova.mmce.common.util.DataReference;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

@Accessors(chain = true)
@Setter
@Getter
@SuppressWarnings("unused")
public class DragBar extends DynamicWidget {

    public static final ResourceLocation DEFAULT_TEX_RES = new ResourceLocation(NovaEngineeringCore.MOD_ID, "textures/gui/dragbar.png");

    public static final int DEFAULT_BAR_HEIGHT = 7;

    public static final int DEFAULT_BAR_PADDING_HORIZONTAL = 10;

    public static final int DEFAULT_BAR_LEFT_TEX_OFFSET_X = 8;
    public static final int DEFAULT_BAR_LEFT_TEX_OFFSET_Y = 1;
    public static final int DEFAULT_BAR_LEFT_TEX_OFFSET_X_FILLED = 0;
    public static final int DEFAULT_BAR_LEFT_TEX_OFFSET_Y_FILLED = 1;

    public static final int DEFAULT_BAR_LEFT_TEX_WIDTH = 3;

    public static final int DEFAULT_BAR_MID_TEX_OFFSET_X = 11;
    public static final int DEFAULT_BAR_MID_TEX_OFFSET_Y = 1;
    public static final int DEFAULT_BAR_MID_TEX_OFFSET_X_FILLED = 3;
    public static final int DEFAULT_BAR_MID_TEX_OFFSET_Y_FILLED = 1;

    public static final int DEFAULT_BAR_RIGHT_TEX_OFFSET_X = 12;
    public static final int DEFAULT_BAR_RIGHT_TEX_OFFSET_Y = 1;
    public static final int DEFAULT_BAR_RIGHT_TEX_OFFSET_X_FILLED = 4;
    public static final int DEFAULT_BAR_RIGHT_TEX_OFFSET_Y_FILLED = 1;

    public static final int DEFAULT_BAR_RIGHT_TEX_WIDTH = 3;

    protected final DragBarButton dragBarButton = new DragBarButton();

    protected int paddingHorizontal = DEFAULT_BAR_PADDING_HORIZONTAL;

    protected ResourceLocation texLocation = DEFAULT_TEX_RES;

    protected int barHeight = DEFAULT_BAR_HEIGHT;

    protected int barLeftTexOffsetX = DEFAULT_BAR_LEFT_TEX_OFFSET_X;
    protected int barLeftTexOffsetY = DEFAULT_BAR_LEFT_TEX_OFFSET_Y;
    protected int barLeftTexOffsetXFilled = DEFAULT_BAR_LEFT_TEX_OFFSET_X_FILLED;
    protected int barLeftTexOffsetYFilled = DEFAULT_BAR_LEFT_TEX_OFFSET_Y_FILLED;
    protected int barLeftTexWidth = DEFAULT_BAR_LEFT_TEX_WIDTH;

    protected int barMidTexOffsetX = DEFAULT_BAR_MID_TEX_OFFSET_X;
    protected int barMidTexOffsetY = DEFAULT_BAR_MID_TEX_OFFSET_Y;
    protected int barMidTexOffsetXFilled = DEFAULT_BAR_MID_TEX_OFFSET_X_FILLED;
    protected int barMidTexOffsetYFilled = DEFAULT_BAR_MID_TEX_OFFSET_Y_FILLED;

    protected int barRightTexOffsetX = DEFAULT_BAR_RIGHT_TEX_OFFSET_X;
    protected int barRightTexOffsetY = DEFAULT_BAR_RIGHT_TEX_OFFSET_Y;
    protected int barRightTexOffsetXFilled = DEFAULT_BAR_RIGHT_TEX_OFFSET_X_FILLED;
    protected int barRightTexOffsetYFilled = DEFAULT_BAR_RIGHT_TEX_OFFSET_Y_FILLED;
    protected int barRightTexWidth = DEFAULT_BAR_RIGHT_TEX_WIDTH;

    protected DataReference<Double> value;
    protected DataReference<Double> min;
    protected DataReference<Double> max;

    public DragBar(final DataReference<Double> value, final DataReference<Double> min, final DataReference<Double> max) {
        this.value = value;
        this.min = min;
        this.max = max;

        this.width = 100;
        this.height = barHeight + 2;
    }

    @Override
    public void initWidget(final WidgetGui gui) {
        dragBarButton.initWidget(gui);
    }

    @Override
    public void update(final WidgetGui gui) {
        dragBarButton.update(gui);
    }

    @Override
    public void preRender(final WidgetGui gui, final RenderSize renderSize, final RenderPos renderPos, final MousePos mousePos) {
        if (!dragBarButton.isMouseDown()) {
            return;
        }

        int width = getWidth() - (paddingHorizontal * 2);
        int mouseX = mousePos.mouseX() - paddingHorizontal;

        if (mouseX <= 0) {
            value.setValue(0D);
            return;
        }

        if (mouseX >= width) {
            value.setValue(max.getValue());
            return;
        }

        float percent = (float) mouseX / width;
        value.setValue(max.getValue() * percent);
    }

    @Override
    public void render(final WidgetGui widgetGui, final RenderSize renderSize, final RenderPos renderPos, final MousePos mousePos) {
        GuiScreen gui = widgetGui.getGui();
        gui.mc.getTextureManager().bindTexture(texLocation);

        int width = getWidth() - (paddingHorizontal * 2);
        int height = getHeight();

        int renderX = renderPos.posX() + paddingHorizontal;
        int renderY = renderPos.posY() + ((height - barHeight) / 2);
        int offsetX = 0;

        double max = this.max.getValue();
        double percent = Math.min(this.value.getValue(), max) / max;

        int toFillFinal = (int) Math.round(width * percent);
        int toFill = toFillFinal;

        // LEFT
        if (toFill >= barLeftTexWidth) {
            gui.drawTexturedModalRect(renderX, renderY, barLeftTexOffsetXFilled, barLeftTexOffsetYFilled, barLeftTexWidth, barHeight);

            offsetX += barLeftTexWidth;
            toFill -= barLeftTexWidth;
        } else {
            gui.drawTexturedModalRect(renderX, renderY, barLeftTexOffsetX, barLeftTexOffsetY, barLeftTexWidth, barHeight);
            if (toFill > 0) {
                gui.drawTexturedModalRect(renderX, renderY, barLeftTexOffsetXFilled, barLeftTexOffsetYFilled, toFill, barHeight);
            }

            offsetX += barLeftTexWidth;
            toFill = 0;
        }

        // MID
        while (toFill > 0 && offsetX <= width - barRightTexWidth) {
            gui.drawTexturedModalRect(renderX + offsetX, renderY, barMidTexOffsetXFilled, barMidTexOffsetYFilled, 1, barHeight);

            offsetX++;
            toFill--;
        }
        while (offsetX <= width - barRightTexWidth) {
            gui.drawTexturedModalRect(renderX + offsetX, renderY, barMidTexOffsetX, barMidTexOffsetY, 1, barHeight);
            offsetX++;
        }

        // RIGHT
        if (toFill >= barRightTexWidth) {
            gui.drawTexturedModalRect(renderX + offsetX, renderY, barRightTexOffsetXFilled, barRightTexOffsetYFilled, barRightTexWidth, barHeight);
        } else {
            gui.drawTexturedModalRect(renderX + offsetX, renderY, barRightTexOffsetX, barRightTexOffsetY, barRightTexWidth, barHeight);
            if (toFill > 0) {
                gui.drawTexturedModalRect(renderX + offsetX, renderY, barRightTexOffsetXFilled, barRightTexOffsetYFilled, toFill, barHeight);
            }
        }

        int dragBarButtonWidth = dragBarButton.getWidth();
        int dragBarButtonHeight = dragBarButton.getHeight();
        RenderPos dragBarButtonRenderOffset = new RenderPos(toFillFinal - (dragBarButtonWidth / 2) + paddingHorizontal, (height - dragBarButtonHeight) / 2);
        dragBarButton.render(widgetGui, renderSize, renderPos.add(dragBarButtonRenderOffset), mousePos.relativeTo(dragBarButtonRenderOffset));
    }

    @Override
    public boolean onMouseClick(final MousePos mousePos, final RenderPos renderPos, final int mouseButton) {
        int width = getWidth() - (paddingHorizontal * 2);

        double max = this.max.getValue();
        double percent = Math.min(this.value.getValue(), max) / max;

        int toFill = (int) Math.round(width * percent);

        int dragBarButtonWidth = dragBarButton.getWidth();
        int dragBarButtonHeight = dragBarButton.getHeight();
        RenderPos dragBarButtonRenderOffset = new RenderPos(toFill - (dragBarButtonWidth / 2) + paddingHorizontal, (height - dragBarButtonHeight) / 2);
        MousePos relativeMousePos = mousePos.relativeTo(dragBarButtonRenderOffset);
        if (dragBarButton.isMouseOver(relativeMousePos)) {
            return dragBarButton.onMouseClick(relativeMousePos, renderPos.add(dragBarButtonRenderOffset), mouseButton);
        }

        return false;
    }

    @Override
    public boolean onMouseReleased(final MousePos mousePos, final RenderPos renderPos) {
        return dragBarButton.onMouseReleased(mousePos, renderPos);
    }

    @Override
    public int getWidth() {
        return super.getWidth() + paddingHorizontal * 2;
    }

    @Accessors(chain = true)
    @Setter
    public class DragBarButton extends DynamicWidget {
        public static final int DEFAULT_BUTTON_HEIGHT = 9;

        public static final int DEFAULT_BUTTON_LEFT_TEX_OFFSET_X = 16;
        public static final int DEFAULT_BUTTON_LEFT_TEX_OFFSET_Y = 0;
        public static final int DEFAULT_BUTTON_LEFT_TEX_WIDTH = 3;

        public static final int DEFAULT_BUTTON_MID_TEX_OFFSET_X = 19;
        public static final int DEFAULT_BUTTON_MID_TEX_OFFSET_Y = 0;

        public static final int DEFAULT_BUTTON_RIGHT_TEX_OFFSET_X = 20;
        public static final int DEFAULT_BUTTON_RIGHT_TEX_OFFSET_Y = 0;
        public static final int DEFAULT_BUTTON_RIGHT_TEX_WIDTH = 3;

        public static final int DEFAULT_ANIMATION_DURATION = 250;

        public static final float MOUSE_OVER_DARK_VALUE = 0.15F;
        public static final float MOUSE_DOWN_DARK_VALUE = 0.3F;

        @Getter
        protected ResourceLocation texLocation = DEFAULT_TEX_RES;

        protected float width = 0;
        protected float height;

        @Getter
        protected int buttonLeftTexOffsetX = DEFAULT_BUTTON_LEFT_TEX_OFFSET_X;
        @Getter
        protected int buttonLeftTexOffsetY = DEFAULT_BUTTON_LEFT_TEX_OFFSET_Y;
        @Getter
        protected int buttonLeftTexWidth = DEFAULT_BUTTON_LEFT_TEX_WIDTH;

        @Getter
        protected int buttonMidTexOffsetX = DEFAULT_BUTTON_MID_TEX_OFFSET_X;
        @Getter
        protected int buttonMidTexOffsetY = DEFAULT_BUTTON_MID_TEX_OFFSET_Y;

        @Getter
        protected int buttonRightTexOffsetX = DEFAULT_BUTTON_RIGHT_TEX_OFFSET_X;
        @Getter
        protected int buttonRightTexOffsetY = DEFAULT_BUTTON_RIGHT_TEX_OFFSET_Y;
        @Getter
        protected int buttonRightTexWidth = DEFAULT_BUTTON_RIGHT_TEX_WIDTH;

        @Getter
        protected int animationDuration = DEFAULT_ANIMATION_DURATION;

        protected double cachedValue;

        @Getter
        protected long expandAnimationStartTime = 0;
        @Getter
        protected boolean expandAnimationStarted = false;

        @Getter
        protected float expandedWidth = 0;
        @Getter
        protected float lastExpandedWidth = 0;

        @Getter
        protected long lastColorUpdateTime = 0;
        @Getter
        protected float darkValue = 0;

        @Getter
        protected boolean mouseOver = false;
        @Getter
        protected boolean mouseDown = false;

        public DragBarButton() {
            this.height = DEFAULT_BUTTON_HEIGHT;
        }

        @Override
        public void initWidget(final WidgetGui gui) {
            this.cachedValue = value.getValue();

            this.expandedWidth = getContentWidth(gui);
            this.lastExpandedWidth = expandedWidth;
            this.width = buttonLeftTexWidth + expandedWidth + buttonRightTexWidth;
        }

        @Override
        public void update(final WidgetGui gui) {
            super.update(gui);

            updateExpandAnimation(gui);
            updateColorAnimation();
        }

        protected void updateExpandAnimation(final WidgetGui gui) {
            Double currentValue = value.getValue();
            if (cachedValue != currentValue) {
                cachedValue = currentValue;
                if (!expandAnimationStarted) {
                    lastExpandedWidth = expandedWidth;
                    expandAnimationStartTime = System.currentTimeMillis();
                    expandAnimationStarted = true;
                }
                return;
            }

            if (!expandAnimationStarted || expandAnimationStartTime == 0) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            int requiredWidth = getContentWidth(gui);

            if (expandAnimationStartTime + animationDuration < currentTime) {
                this.expandedWidth = requiredWidth;
                this.width = buttonLeftTexWidth + expandedWidth + buttonRightTexWidth;
                this.expandAnimationStarted = false;
                return;
            }

            float animationPercent = (float) (currentTime - expandAnimationStartTime) / animationDuration;
            float toExpand = (requiredWidth - lastExpandedWidth) * animationPercent;

            this.expandedWidth = lastExpandedWidth + toExpand;
            this.width = buttonLeftTexWidth + expandedWidth + buttonRightTexWidth;
        }

        protected void updateColorAnimation() {
            float requiredValue = mouseDown ? MOUSE_DOWN_DARK_VALUE : mouseOver ? MOUSE_OVER_DARK_VALUE : 0;
            if (requiredValue == darkValue) {
                return;
            }

            long currentTime = System.currentTimeMillis();

            int timeLags = (int) (currentTime - lastColorUpdateTime);
            if (timeLags <= 0) {
                return;
            }

            float animationValue = (float) timeLags / (animationDuration * 10F);
            if (requiredValue > darkValue) {
                darkValue = Math.min(darkValue + animationValue, requiredValue);
            } else {
                darkValue = Math.max(darkValue - animationValue, requiredValue);
            }
        }

        @Override
        public void render(final WidgetGui widgetGui, final RenderSize renderSize, final RenderPos renderPos, final MousePos mousePos) {
            GuiScreen gui = widgetGui.getGui();

            if (mouseOver) {
                if (!isMouseOver(mousePos)) {
                    mouseOver = false;
                    lastColorUpdateTime = System.currentTimeMillis();
                }
            } else {
                if (isMouseOver(mousePos)) {
                    mouseOver = true;
                    lastColorUpdateTime = System.currentTimeMillis();
                }
            }

            gui.mc.getTextureManager().bindTexture(texLocation);

            int contentWidth = getContentWidth(widgetGui);

            float width = this.width;
            float height = this.height;

            float offsetX = ((int) width - width) / 2F;

            // Dark Animation
            GlStateManager.color(1.0F - darkValue, 1.0F - darkValue, 1.0F - darkValue);
            // Render Button
            gui.drawTexturedModalRect(renderPos.posX() + offsetX, renderPos.posY(), buttonLeftTexOffsetX, buttonLeftTexOffsetY, buttonLeftTexWidth, (int) height);
            offsetX += buttonLeftTexWidth;

            while (offsetX + buttonRightTexWidth < width) {
                gui.drawTexturedModalRect(renderPos.posX() + offsetX, renderPos.posY(), buttonMidTexOffsetX, buttonMidTexOffsetY, 1, (int) height);
                offsetX++;
            }
            if (offsetX < width - buttonRightTexWidth) {
                gui.drawTexturedModalRect(renderPos.posX() + offsetX - (width - buttonRightTexWidth - offsetX), renderPos.posY(), buttonMidTexOffsetX, buttonMidTexOffsetY, 1, (int) height);
                offsetX += (width - buttonRightTexWidth - offsetX);
            }
            gui.drawTexturedModalRect(renderPos.posX() + offsetX, renderPos.posY(), buttonRightTexOffsetX, buttonRightTexOffsetY, buttonRightTexWidth, (int) height);
            GlStateManager.color(1.0F, 1.0F, 1.0F);

            // Render Content
            String formattedValue = NovaEngUtils.formatDouble(cachedValue, 2);
            gui.mc.fontRenderer.drawString(formattedValue, (float) renderPos.posX() + ((width - contentWidth) / 2F), (float) renderPos.posY(), 0xFFFFFF, false);
        }

        @Override
        public int getWidth() {
            return (int) width;
        }

        @Override
        public DragBarButton setWidth(final int width) {
            this.width = width;
            return this;
        }

        @Override
        public int getHeight() {
            return (int) height;
        }

        @Override
        public DragBarButton setHeight(final int height) {
            this.height = height;
            return this;
        }

        @Override
        public boolean onMouseClick(final MousePos mousePos, final RenderPos renderPos, final int mouseButton) {
            mouseDown = true;
            lastColorUpdateTime = System.currentTimeMillis();
            return true;
        }

        @Override
        public boolean onMouseReleased(final MousePos mousePos, final RenderPos renderPos) {
            mouseDown = false;
            lastColorUpdateTime = System.currentTimeMillis();
            return false;
        }

        protected int getContentWidth(final WidgetGui widgetGui) {
            String formattedValue = NovaEngUtils.formatDouble(cachedValue, 2);
            return widgetGui.getGui().mc.fontRenderer.getStringWidth(formattedValue);
        }

        public DragBarButton setTexLocation(final ResourceLocation texLocation) {
            this.texLocation = texLocation;
            return this;
        }

        public DragBarButton setButtonLeftTexOffsetX(final int buttonLeftTexOffsetX) {
            this.buttonLeftTexOffsetX = buttonLeftTexOffsetX;
            return this;
        }

        public DragBarButton setButtonLeftTexOffsetY(final int buttonLeftTexOffsetY) {
            this.buttonLeftTexOffsetY = buttonLeftTexOffsetY;
            return this;
        }

        public DragBarButton setButtonLeftTexWidth(final int buttonLeftTexWidth) {
            this.buttonLeftTexWidth = buttonLeftTexWidth;
            return this;
        }

        public DragBarButton setButtonMidTexOffsetX(final int buttonMidTexOffsetX) {
            this.buttonMidTexOffsetX = buttonMidTexOffsetX;
            return this;
        }

        public DragBarButton setButtonMidTexOffsetY(final int buttonMidTexOffsetY) {
            this.buttonMidTexOffsetY = buttonMidTexOffsetY;
            return this;
        }

        public DragBarButton setButtonRightTexOffsetX(final int buttonRightTexOffsetX) {
            this.buttonRightTexOffsetX = buttonRightTexOffsetX;
            return this;
        }

        public DragBarButton setButtonRightTexOffsetY(final int buttonRightTexOffsetY) {
            this.buttonRightTexOffsetY = buttonRightTexOffsetY;
            return this;
        }

        public DragBarButton setButtonRightTexWidth(final int buttonRightTexWidth) {
            this.buttonRightTexWidth = buttonRightTexWidth;
            return this;
        }

        public DragBarButton setAnimationDuration(final int animationDuration) {
            this.animationDuration = animationDuration;
            return this;
        }

        public DragBarButton setExpandAnimationStartTime(final long expandAnimationStartTime) {
            this.expandAnimationStartTime = expandAnimationStartTime;
            return this;
        }

        public DragBarButton setExpandAnimationStarted(final boolean expandAnimationStarted) {
            this.expandAnimationStarted = expandAnimationStarted;
            return this;
        }

        public DragBarButton setExpandedWidth(final float expandedWidth) {
            this.expandedWidth = expandedWidth;
            return this;
        }

        public DragBarButton setLastExpandedWidth(final float lastExpandedWidth) {
            this.lastExpandedWidth = lastExpandedWidth;
            return this;
        }

        public DragBarButton setLastColorUpdateTime(final long lastColorUpdateTime) {
            this.lastColorUpdateTime = lastColorUpdateTime;
            return this;
        }

        public DragBarButton setDarkValue(final float darkValue) {
            this.darkValue = darkValue;
            return this;
        }

        public DragBarButton setMouseOver(final boolean mouseOver) {
            this.mouseOver = mouseOver;
            return this;
        }

        public DragBarButton setMouseDown(final boolean mouseDown) {
            this.mouseDown = mouseDown;
            return this;
        }
    }

}
