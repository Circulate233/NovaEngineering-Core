package github.kasuminova.novaeng.mixin.zenutils;

import github.kasuminova.novaeng.common.performance.ScriptClasspathArchives;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import youyihj.zenutils.impl.member.bytecode.ClasspathBytesProvider;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

@Mixin(value = ClasspathBytesProvider.class, remap = false)
public abstract class MixinClasspathBytesPath {
    @Shadow
    @Final
    private List<FileSystem> jars;
    @Shadow
    private boolean closed;
    @Unique
    private int nova$releasedArchives;

    /**
     * @author NovaEngineering
     * @reason Release each archive lease once, including after a repeated or partial close.
     */
    @Overwrite
    public synchronized void close() throws IOException {
        closed = true;
        IOException failure = null;
        while (nova$releasedArchives < jars.size()) {
            try {
                ScriptClasspathArchives.release(jars.get(nova$releasedArchives++));
            } catch (IOException error) {
                if (failure == null) {
                    failure = error;
                } else {
                    failure.addSuppressed(error);
                }
            }
        }
        if (failure != null) throw failure;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
        target = "Ljava/nio/file/Files;walkFileTree(Ljava/nio/file/Path;Ljava/nio/file/FileVisitor;)Ljava/nio/file/Path;"), require = 1)
    private Path nova$visitArchiveDirectly(final Path path, final FileVisitor<Path> visitor) throws IOException {
        // Use the same attributes and visitor as walkFileTree; directories and symlinks retain its traversal.
        final BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(path, BasicFileAttributes.class, java.nio.file.LinkOption.NOFOLLOW_LINKS);
        } catch (IOException failure) {
            return Files.walkFileTree(path, visitor);
        }
        if (attributes.isRegularFile()) {
            visitor.visitFile(path, attributes);
            return path;
        }
        return Files.walkFileTree(path, visitor);
    }

    @Redirect(method = "getClassBytes", at = @At(value = "INVOKE",
        target = "Ljava/lang/String;split(Ljava/lang/String;)[Ljava/lang/String;"), require = 1)
    private String[] nova$joinClassPathOnce(final String className, final String regex) {
        if (!"\\.".equals(regex) || className.startsWith(".") || className.endsWith(".")
            || className.contains("..")) {
            return className.split(regex);
        }
        // The original method appends .class, then passes first/more to every ZIP filesystem.
        // An already joined first component avoids building the same path again for each JAR.
        return new String[]{className.replace('.', '/')};
    }
}
