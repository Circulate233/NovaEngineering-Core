package github.kasuminova.novaeng.mixin.enderio;

import crazypants.enderio.base.config.recipes.RecipeFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.xml.stream.XMLInputFactory;

/**
 * Reuses EnderIO's XML input factory on its single-threaded recipe loading path.
 *
 * <p>The factory is created on the first {@code readStax} call. The target method
 * still creates a fresh {@code XMLEventReader} for every input stream, so XML
 * documents remain independent and no reader crosses a recipe or thread boundary.</p>
 */
@Mixin(value = RecipeFactory.class, remap = false)
public class MixinRecipeFactoryXmlInputFactory {

    @Unique
    private static XMLInputFactory nova$xmlInputFactory;

    @Redirect(
        method = "readStax",
        at = @At(value = "INVOKE",
            target = "Ljavax/xml/stream/XMLInputFactory;newInstance()Ljavax/xml/stream/XMLInputFactory;",
            remap = false),
        require = 1)
    private static XMLInputFactory nova$getXmlInputFactory() {
        XMLInputFactory inputFactory = nova$xmlInputFactory;
        if (inputFactory == null) {
            inputFactory = XMLInputFactory.newInstance();
            nova$xmlInputFactory = inputFactory;
        }
        return inputFactory;
    }
}
