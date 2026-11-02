package github.kasuminova.novaeng.mixin.techguns;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import techguns.world.dungeon.DungeonTemplate;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

@Mixin(value = DungeonTemplate.class, remap = false)
public abstract class MixinTemplateInputBuffer {
    @Redirect(method = {"loadTemplates", "loadTemplates_debug"}, at = @At(value = "NEW",
        target = "java/io/ObjectInputStream"), require = 2)
    private static ObjectInputStream nova$bufferTemplate(final InputStream in) throws IOException {
        // OIS performs many tiny reads; keep them out of the ZIP inflater/file stream.
        return new ObjectInputStream(in == null ? null : new BufferedInputStream(in, 65536));
    }
}
