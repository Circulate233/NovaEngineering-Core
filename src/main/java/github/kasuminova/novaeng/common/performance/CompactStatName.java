package github.kasuminova.novaeng.common.performance;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.List;

/** An owned StatList display template. Copies expose ordinary, independently mutable vanilla components. */
public final class CompactStatName implements ITextComponent {
    private final String key;
    private final String name;
    private final TextFormatting color;
    private final Boolean italic;
    // Vanilla createCopy shallow-copies Style: the original hover event/value must retain its identity.
    private final HoverEvent hover;
    private ITextComponent expanded;

    private CompactStatName(final String key, final String name, final TextFormatting color,
                            final Boolean italic, final HoverEvent hover) {
        this.key = key;
        this.name = name;
        this.color = color;
        this.italic = italic;
        this.hover = hover;
    }

    public static ITextComponent capture(final ITextComponent source) {
        if (source.getClass() != TextComponentTranslation.class) return source;
        final TextComponentTranslation translation = (TextComponentTranslation) source;
        if (!translation.getStyle().isEmpty() || !translation.getSiblings().isEmpty()) return source;
        final Object[] args = translation.getFormatArgs();
        if (args.length != 1 || !(args[0] instanceof TextComponentString bracket)
            || bracket.getClass() != TextComponentString.class || !bracket.getText().equals("[")) return source;
        final List<ITextComponent> children = bracket.getSiblings();
        if (children.size() != 2 || children.getFirst().getClass() != TextComponentString.class
            || children.get(1).getClass() != TextComponentString.class) return source;
        final TextComponentString label = (TextComponentString) children.getFirst();
        final TextComponentString end = (TextComponentString) children.get(1);
        if (!label.getSiblings().isEmpty() || !end.getSiblings().isEmpty() || !end.getText().equals("]")
            || !end.getStyle().isEmpty()) return source;
        final Style itemStyle = bracket.getStyle();
        if (!itemStyle.createShallowCopy().setColor(null).setHoverEvent(null).isEmpty()
            || !label.getStyle().createShallowCopy().setItalic(null).isEmpty()) return source;
        final HoverEvent hover = itemStyle.getHoverEvent();
        final Boolean italic = label.getStyle().isEmpty() ? null : label.getStyle().getItalic();
        return new CompactStatName(translation.getKey(), label.getText(), itemStyle.getColor(), italic, hover);
    }

    private ITextComponent restore() {
        final TextComponentString label = new TextComponentString(name);
        if (italic != null) label.getStyle().setItalic(italic);
        final ITextComponent bracket = new TextComponentString("[").appendSibling(label).appendText("]");
        bracket.getStyle().setColor(color).setHoverEvent(hover);
        return new TextComponentTranslation(key, bracket);
    }

    private ITextComponent mutable() {
        if (expanded == null) expanded = restore();
        return expanded;
    }

    @Override public @NonNull ITextComponent createCopy() { return expanded == null ? restore() : expanded.createCopy(); }
    @Override public @NonNull ITextComponent setStyle(final @NonNull Style style) { mutable().setStyle(style); return this; }
    @Override public @NonNull Style getStyle() { return mutable().getStyle(); }
    @Override public @NonNull ITextComponent appendText(final @NonNull String text) { mutable().appendText(text); return this; }
    @Override public @NonNull ITextComponent appendSibling(final @NonNull ITextComponent component) { mutable().appendSibling(component); return this; }
    @Override public @NonNull String getUnformattedComponentText() { return (expanded == null ? restore() : expanded).getUnformattedComponentText(); }
    @Override public @NonNull String getUnformattedText() { return (expanded == null ? restore() : expanded).getUnformattedText(); }
    @Override public @NonNull String getFormattedText() { return (expanded == null ? restore() : expanded).getFormattedText(); }
    @Override public @NonNull List<ITextComponent> getSiblings() { return mutable().getSiblings(); }
    @Override public @NonNull Iterator<ITextComponent> iterator() { return mutable().iterator(); }
    @Override public String toString() { return (expanded == null ? restore() : expanded).toString(); }
    @Override public int hashCode() { return (expanded == null ? restore() : expanded).hashCode(); }
    @Override public boolean equals(final Object other) {
        final Object value = other instanceof CompactStatName compact ? compact.createCopy() : other;
        return (expanded == null ? restore() : expanded).equals(value);
    }
}
