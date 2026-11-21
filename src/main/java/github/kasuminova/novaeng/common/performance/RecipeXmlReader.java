package github.kasuminova.novaeng.common.performance;

import crazypants.enderio.base.config.recipes.IRecipeConfigElement;
import crazypants.enderio.base.config.recipes.InvalidRecipeConfigException;
import crazypants.enderio.base.config.recipes.StaxFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Iterator;

public final class RecipeXmlReader {
    private RecipeXmlReader() {
    }

    public static <T extends IRecipeConfigElement> T read(final StaxFactory factory, final XMLEventReader reader,
                                                         final String source, final T target,
                                                         final StartElement start) throws InvalidRecipeConfigException, XMLStreamException {
        target.setSource(source != null ? source : "unkown");
        try {
            final Iterator<?> attributes = start.getAttributes();
            while (attributes.hasNext()) {
                final Attribute attribute = (Attribute) attributes.next();
                final String value = attribute.getValue();
                if (!target.setAttribute(factory, attribute.getName().getLocalPart(), value != null ? value : "")) {
                    throw new InvalidRecipeConfigException("Unexpected attribute '" + attribute.getName() + "' inside " + start.getName());
                }
            }
        } catch (InvalidRecipeConfigException failure) {
            throw withLocation(failure, start);
        }
        while (reader.hasNext()) {
            final XMLEvent event = reader.nextEvent();
            try {
                if (event.isStartElement()) {
                    final StartElement child = event.asStartElement();
                    final String name = child.getName().getLocalPart();
                    if (!target.setElement(factory, name != null ? name : "", child)) {
                        throw new InvalidRecipeConfigException("Unexpected tag '" + child.getName() + "' inside " + start.getName());
                    }
                } else if (event.isEndElement()) {
                    target.readResolve();
                    return target;
                } else if ((!event.isCharacters() || !event.asCharacters().isWhiteSpace()) && event.getEventType() != 5) {
                    throw new InvalidRecipeConfigException("Unexpected '" + event.getEventType() + "' inside " + start.getName());
                }
            } catch (InvalidRecipeConfigException failure) {
                throw withLocation(failure, event);
            }
        }
        throw new InvalidRecipeConfigException("Unexpected end of document inside " + start.getName());
    }

    private static InvalidRecipeConfigException withLocation(final InvalidRecipeConfigException failure,
                                                              final XMLEvent event) {
        if (failure.getMessage().contains("[row,col]")) {
            return failure;
        }
        return new InvalidRecipeConfigException(failure,
            "at [row,col]:[" + event.getLocation().getLineNumber() + "," + event.getLocation().getColumnNumber() + "]");
    }
}
