package github.kasuminova.novaeng.mixin.codechickenlib;

import codechicken.lib.model.loader.blockstate.CCBlockStateLoader;
import github.kasuminova.novaeng.common.performance.ArchiveResourceIndex;
import net.minecraftforge.fml.common.ModContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;

@Mixin(value = CCBlockStateLoader.class, remap = false)
public abstract class MixinCclFactoryProbe {
    @Redirect(method = "loadFactories(Lnet/minecraftforge/fml/common/ModContainer;)V",
        at = @At(value = "INVOKE", target = "Ljava/io/File;isFile()Z"), require = 1)
    private static boolean nova$hasFactories(final File source, final ModContainer mod) {
        return source.isFile() && (!source.getName().endsWith(".jar")
            || ArchiveResourceIndex.mayContain(source, "assets/" + mod.getModId() + "/cc_blockstates/_factories.json"));
    }
}
