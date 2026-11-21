package github.kasuminova.novaeng.mixin.base;

import com.teamacronymcoders.base.util.files.DirectoryResourcePack;
import github.kasuminova.novaeng.client.resource.DirectoryExistenceCache;
import net.minecraft.client.resources.AbstractResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.io.File;

/**
 * Answers directory backed resource probes from the reload scoped existence cache.
 *
 * Ordinary reload probes use their logical names directly; unusual paths keep BASE's filesystem resolution.
 */
@Mixin(DirectoryResourcePack.class)
public abstract class MixinDirectoryResourcePack extends AbstractResourcePack {

    protected MixinDirectoryResourcePack(final File resourcePackFile) {
        super(resourcePackFile);
    }

    /**
     * @author Kasumi_Nova
     * @reason Avoid constructing a File and replacement String for each reload probe.
     */
    @Overwrite
    @Override
    protected boolean hasResourceName(final String name) {
        return DirectoryExistenceCache.instance().existsResource(this.resourcePackFile, name);
    }
}
