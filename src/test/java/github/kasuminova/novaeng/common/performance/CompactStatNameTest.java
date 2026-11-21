package github.kasuminova.novaeng.common.performance;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompactStatNameTest {
    private static TextComponentTranslation template(final String name, final Boolean italic) {
        final TextComponentString label = new TextComponentString(name);
        if (italic != null) label.getStyle().setItalic(italic);
        final ITextComponent bracket = new TextComponentString("[").appendSibling(label).appendText("]");
        bracket.getStyle().setColor(TextFormatting.AQUA)
            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new TextComponentString("{id:\"test:item\"}")));
        return new TextComponentTranslation("stat.craftItem", bracket);
    }

    @Test
    void capturesOnlyOwnedVanillaShapeAndPreservesStyleAndHoverCopyContract() {
        for (final Boolean italic : new Boolean[]{null, Boolean.TRUE, Boolean.FALSE}) {
            final TextComponentTranslation source = template("测试 🔥", italic);
            final ITextComponent compact = CompactStatName.capture(source);
            assertInstanceOf(CompactStatName.class, compact);
            final ITextComponent copy = compact.createCopy();
            assertEquals(ITextComponent.Serializer.componentToJson(source.createCopy()), ITextComponent.Serializer.componentToJson(copy));
            assertEquals(source.toString(), compact.toString());
            final ITextComponent originalArg = (ITextComponent) source.getFormatArgs()[0];
            final ITextComponent copiedArg = (ITextComponent) ((TextComponentTranslation) copy).getFormatArgs()[0];
            assertSame(originalArg.getStyle().getHoverEvent(), copiedArg.getStyle().getHoverEvent());
            copy.getStyle().setBold(true).setColor(TextFormatting.GRAY);
            final ITextComponent second = compact.createCopy();
            assertFalse(second.getStyle().getBold());
            assertNotSame(copy, second);
            assertEquals(source.createCopy(), second);
        }
    }

    @Test
    void unknownDisplayShapesKeepTheOriginalAndPublicMutationExpandsLazily() {
        final TextComponentString custom = new TextComponentString("custom");
        assertSame(custom, CompactStatName.capture(custom));
        final TextComponentTranslation styled = template("item", null);
        styled.getStyle().setUnderlined(true);
        assertSame(styled, CompactStatName.capture(styled));
        final ITextComponent compact = CompactStatName.capture(template("item", null));
        compact.appendText(" suffix").getStyle().setBold(true);
        assertTrue(compact.createCopy().getStyle().getBold());
        assertEquals(1, compact.getSiblings().size());
    }
}
