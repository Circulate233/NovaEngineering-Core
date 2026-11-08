package github.kasuminova.novaeng.mixin.stellarcore;

import github.kasuminova.stellarcore.client.resource.DirectoryPathIndex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = DirectoryPathIndex.class, remap = false)
public interface InvokerDirectoryPathIndex {
    @Invoker("normalizePath")
    static String nova$normalizePath(final String path) {
        throw new AssertionError();
    }
}
