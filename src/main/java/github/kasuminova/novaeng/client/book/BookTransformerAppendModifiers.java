package github.kasuminova.novaeng.client.book;

import github.kasuminova.novaeng.common.trait.Register;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.PageData;
import slimeknights.mantle.client.book.data.SectionData;
import slimeknights.mantle.client.book.repository.BookRepository;
import slimeknights.mantle.client.book.repository.FileRepository;
import slimeknights.tconstruct.library.book.content.ContentListing;
import slimeknights.tconstruct.library.book.sectiontransformer.SectionTransformer;
import slimeknights.tconstruct.library.modifiers.Modifier;

import java.util.List;
import java.util.RandomAccess;

// adapted from Tinkers' MEMES BookTransformerAppendModifiers
public class BookTransformerAppendModifiers extends SectionTransformer {

    public static BookTransformerAppendModifiers INSTANCE_FALSE = new BookTransformerAppendModifiers(new FileRepository("tconstruct:book"), false, Register.INSTANCE.getModifierTraitsF());
    public static BookTransformerAppendModifiers INSTANCE_TRUE = new BookTransformerAppendModifiers(new FileRepository("tconstruct:book"), true, Register.INSTANCE.getModifierTraitsT());
    private final BookRepository source;
    private final boolean armour;
    private final List<Modifier> modCollector;

    public BookTransformerAppendModifiers(BookRepository source, boolean armour, List<Modifier> modCollector) {
        super("modifiers");
        this.source = source;
        this.armour = armour;
        this.modCollector = modCollector;
    }

    @Override
    public void transform(BookData book, SectionData section) {
        ContentListing listing = (ContentListing) section.pages.getFirst().content;
        if (modCollector instanceof RandomAccess) {
            for (int index = 0; index < modCollector.size(); index++) {
                appendModifier(listing, section, modCollector.get(index));
            }
        } else {
            for (final Modifier modifier : modCollector) appendModifier(listing, section, modifier);
        }
    }

    private void appendModifier(final ContentListing listing, final SectionData section, final Modifier modifier) {
        PageData page = new PageData();
        page.source = source;
        page.parent = section;
        page.type = armour ? "armormodifier" : "modifier";
        page.data = "modifiers/" + modifier.identifier + ".json";
        section.pages.add(page);
        page.load();
        listing.addEntry(modifier.getLocalizedName(), page);
    }
}
