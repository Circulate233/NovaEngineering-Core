package github.kasuminova.novaeng.mixin.enderio;

import crazypants.enderio.base.config.recipes.IRecipeConfigElement;
import crazypants.enderio.base.config.recipes.InvalidRecipeConfigException;
import crazypants.enderio.base.config.recipes.StaxFactory;
import github.kasuminova.novaeng.common.performance.RecipeXmlReader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;

@Mixin(value = StaxFactory.class, remap = false)
public abstract class MixinStaxFactory {
    @Shadow @Final private XMLEventReader eventReader;
    @Shadow @Final private String source;

    /**
     * @author circulation
     * @reason Avoid per-attribute fallback arrays and repeated event conversion while preserving callbacks and errors.
     */
    @Overwrite
    public <T extends IRecipeConfigElement> T read(final T target, final StartElement start)
        throws InvalidRecipeConfigException, XMLStreamException {
        return RecipeXmlReader.read((StaxFactory) (Object) this, eventReader, source, target, start);
    }
}
