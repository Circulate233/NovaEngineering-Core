package github.kasuminova.novaeng.mixin.avaritia;

import github.kasuminova.novaeng.common.performance.ArchiveResourceIndex;
import morph.avaritia.recipe.AvaritiaRecipeManager;
import net.minecraftforge.fml.common.ModContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;

@Mixin(value = AvaritiaRecipeManager.class, remap = false)
public abstract class MixinAvaritiaFactoryProbe {
    @Redirect(method = "loadFactories", at = @At(value = "INVOKE", target = "Ljava/io/File;isFile()Z"), require = 1)
    private static boolean nova$hasFactories(final File source, final ModContainer mod) {
        return source.isFile() && (!source.getName().endsWith(".jar")
            || ArchiveResourceIndex.mayContain(source, "assets/" + mod.getModId() + "/avaritia_recipes/_factories.json"));
    }
}
