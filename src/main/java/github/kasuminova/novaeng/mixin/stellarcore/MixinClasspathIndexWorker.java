package github.kasuminova.novaeng.mixin.stellarcore;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// NamespaceIndex is a private nested class and cannot be named by a Java class literal.
@Mixin(targets = "github.kasuminova.stellarcore.client.resource.ClasspathAssetIndex$NamespaceIndex", remap = false)
public abstract class MixinClasspathIndexWorker {

    @ModifyExpressionValue(method = "scanJars", at = @At(value = "INVOKE",
        target = "Lgithub/kasuminova/stellarcore/client/resource/ClasspathAssetIndex;access$900()I"), require = 1)
    private int nova$avoidNestedExecutorWait(final int threads) {
        return Thread.currentThread().getName().startsWith("StellarCore-ClasspathAssetIndex-") ? 1 : threads;
    }
}
