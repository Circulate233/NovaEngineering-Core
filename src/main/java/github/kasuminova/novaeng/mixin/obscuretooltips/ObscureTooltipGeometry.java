package github.kasuminova.novaeng.mixin.obscuretooltips;

import com.anthonyhilyard.legendarytooltips.LegendaryTooltips;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig;
import dev.obscuria.tooltips.client.tooltip.TooltipStyle;
import dev.obscuria.tooltips.client.tooltip.element.frame.NineSlicedFrame;
import dev.obscuria.tooltips.client.tooltip.element.frame.TooltipFrame;
import dev.obscuria.tooltips.client.tooltip.element.panel.ColorRectPanel;
import dev.obscuria.tooltips.client.tooltip.element.panel.TooltipPanel;
import dev.obscuria.tooltips.util.color.ARGB;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.Collections;
import java.util.List;

public final class ObscureTooltipGeometry {

    /** Legendary Tooltips 的装饰相对它收到矩形的偏移量，见 {@code TooltipDecor#drawBorder}。 */
    private static final int DECOR_HORIZONTAL = 6;
    private static final int DECOR_VERTICAL = 9;

    /** {@code NineSlicedFrame#render} 里硬编码的外扩量，四边统一。 */
    private static final int NINE_SLICED_OVERHANG = 31;
    /** {@code ColorRectPanel#render} 最外侧边框相对面板矩形的外扩量。 */
    private static final int COLOR_RECT_OVERHANG = 4;

    private static final int MIN_EVENT_SIZE = 8;

    /** Forge 原版 {@code GuiUtils#drawHoveringText} 传入的默认配色，仅作兜底。 */
    private static final int VANILLA_BACKGROUND = 0xF0100010;
    private static final int VANILLA_BORDER_START = 0x505000FF;
    private static final int VANILLA_BORDER_END = 0x5028007F;

    private static boolean panelCaptured;
    private static int panelX;
    private static int panelY;
    private static int panelWidth;
    private static int panelHeight;
    private static int overhang;

    private static List<String> rawLines = Collections.emptyList();

    private static boolean tintResolved;
    private static int tintColor = 0xFFFFFFFF;
    private static boolean tintInScope;

    private ObscureTooltipGeometry() {
    }

    public static void beginRender(final List<String> lines) {
        panelCaptured = false;
        tintResolved = false;
        tintInScope = false;
        rawLines = lines == null ? Collections.emptyList() : lines;
    }

    public static void capturePanel(final TooltipStyle style, final int x, final int y, final int width, final int height) {
        panelX = x;
        panelY = y;
        panelWidth = width;
        panelHeight = height;
        overhang = resolveOverhang(style);
        panelCaptured = true;
    }

    private static int resolveOverhang(final TooltipStyle style) {
        final int configured = NovaEngCoreConfig.CLIENT.tooltipFrameOverhang;
        if (configured >= 0) {
            return configured;
        }
        final TooltipFrame frame = style.frame().orElse(null);
        if (frame instanceof NineSlicedFrame) {
            return NINE_SLICED_OVERHANG;
        }
        final TooltipPanel panel = style.panel().orElse(null);
        if (panel instanceof ColorRectPanel) {
            return COLOR_RECT_OVERHANG;
        }
        return 0;
    }

    public static int eventX() {
        return panelX + DECOR_HORIZONTAL - overhang;
    }

    public static int eventY() {
        return panelY + DECOR_VERTICAL - overhang;
    }

    public static int eventWidth() {
        return Math.max(panelWidth + 2 * overhang - 2 * DECOR_HORIZONTAL, MIN_EVENT_SIZE);
    }

    public static int eventHeight() {
        return Math.max(panelHeight + 2 * overhang - 2 * DECOR_VERTICAL, MIN_EVENT_SIZE);
    }

    public static Event remapPostEvent(final Event event) {
        if (!panelCaptured) {
            return event;
        }
        if (event instanceof RenderTooltipEvent.PostBackground post) {
            return new RenderTooltipEvent.PostBackground(post.getStack(), post.getLines(),
                eventX(), eventY(), post.getFontRenderer(), eventWidth(), eventHeight());
        }
        if (event instanceof RenderTooltipEvent.PostText post) {
            return new RenderTooltipEvent.PostText(post.getStack(), post.getLines(),
                eventX(), eventY(), post.getFontRenderer(), eventWidth(), eventHeight());
        }
        return event;
    }

    /** 借用 Legendary Tooltips 自己的配色逻辑，同时修正它那份会被其它提示框污染的静态边框颜色。 */
    public static void resolveTint(final ItemStack stack) {
        tintResolved = false;
        if (stack == null || stack.isEmpty()) {
            return;
        }
        final LegendaryTooltipsConfig config = LegendaryTooltipsConfig.INSTANCE;
        if (config == null) {
            return;
        }
        // 没有自定义等级且不匹配稀有度时，Legendary Tooltips 只会回退到原版默认色，叠上去会把面板压黑。
        if (config.getFrameLevelForItem(stack) == LegendaryTooltips.STANDARD && !config.bordersMatchRarity) {
            return;
        }
        final RenderTooltipEvent.Color event = new RenderTooltipEvent.Color(stack, rawLines, panelX, panelY,
            Minecraft.getMinecraft().fontRenderer, VANILLA_BACKGROUND, VANILLA_BORDER_START, VANILLA_BORDER_END);
        MinecraftForge.EVENT_BUS.post(event);

        final int color = event.getBorderStart();
        if ((color >>> 24) == 0) {
            return;
        }
        tintColor = color;
        tintResolved = true;
    }

    public static void beginTintScope() {
        tintInScope = true;
    }

    public static void endTintScope() {
        tintInScope = false;
    }

    /** 正片叠底，alpha 保持不变，否则会连带把面板变得透明。 */
    public static ARGB tint(final ARGB color) {
        if (color == null) {
            return null;
        }
        final float strength = tintStrength();
        if (strength <= 0.0F) {
            return color;
        }
        return new ARGB(color.alpha(),
            color.red() * channelMultiplier(tintColor >> 16, strength),
            color.green() * channelMultiplier(tintColor >> 8, strength),
            color.blue() * channelMultiplier(tintColor, strength));
    }

    public static ARGB frameTint() {
        final float strength = tintStrength();
        if (strength <= 0.0F) {
            return null;
        }
        return new ARGB(1.0F,
            channelMultiplier(tintColor >> 16, strength),
            channelMultiplier(tintColor >> 8, strength),
            channelMultiplier(tintColor, strength));
    }

    private static float tintStrength() {
        if (!tintInScope || !tintResolved) {
            return 0.0F;
        }
        return (float) NovaEngCoreConfig.CLIENT.tooltipTintStrength;
    }

    /** strength 为 0 时返回 1.0（保持原色），为 1 时返回该通道自身的值。 */
    private static float channelMultiplier(final int argb, final float strength) {
        final float target = (argb & 0xFF) / 255.0F;
        return 1.0F + (target - 1.0F) * strength;
    }
}
