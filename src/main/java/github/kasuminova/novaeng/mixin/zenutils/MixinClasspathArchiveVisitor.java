package github.kasuminova.novaeng.mixin.zenutils;

import github.kasuminova.novaeng.common.performance.ScriptClasspathArchives;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

@Mixin(targets = "youyihj.zenutils.impl.member.bytecode.ClasspathBytesProvider$1", remap = false)
public abstract class MixinClasspathArchiveVisitor {
    @Redirect(method = "visitFile(Ljava/nio/file/Path;Ljava/nio/file/attribute/BasicFileAttributes;)Ljava/nio/file/FileVisitResult;",
        at = @At(value = "INVOKE", target = "Ljava/nio/file/FileSystems;newFileSystem(Ljava/nio/file/Path;Ljava/lang/ClassLoader;)Ljava/nio/file/FileSystem;"),
        require = 1)
    private FileSystem nova$openArchive(final Path path, final ClassLoader loader) throws IOException {
        return ScriptClasspathArchives.open(path, loader);
    }
}
