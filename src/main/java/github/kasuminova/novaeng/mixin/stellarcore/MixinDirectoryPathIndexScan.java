package github.kasuminova.novaeng.mixin.stellarcore;

import github.kasuminova.novaeng.common.performance.DirectoryTreeScan;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.io.IOException;

@Mixin(targets = "github.kasuminova.stellarcore.client.resource.DirectoryPathIndex$Index", remap = false)
public abstract class MixinDirectoryPathIndexScan {
    @Shadow @Final private File root;
    @Shadow @Final private NonBlockingHashSet<String> paths;

    @Shadow
    private boolean isCurrent() {
        throw new AssertionError();
    }

    /**
     * @author circulation
     * @reason Use directory-entry attributes and retain the existing generation, normalization and failure policy.
     */
    @Overwrite
    private void scan() throws IOException {
        DirectoryTreeScan.scan(root, this::isCurrent, path -> paths.add(InvokerDirectoryPathIndex.nova$normalizePath(path)));
    }
}
