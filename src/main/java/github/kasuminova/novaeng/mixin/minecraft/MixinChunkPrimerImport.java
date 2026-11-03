package github.kasuminova.novaeng.mixin.minecraft;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import github.kasuminova.novaeng.common.performance.ChunkPrimerImport;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Chunk.class)
public abstract class MixinChunkPrimerImport {
    @Unique
    private boolean nova$primerImported;

    @ModifyExpressionValue(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/world/chunk/ChunkPrimer;II)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProvider;hasSkyLight()Z"), require = 1)
    private boolean nova$importSections(final boolean skyLight, final World world,
                                        final ChunkPrimer primer, final int x, final int z) {
        nova$primerImported = ChunkPrimerImport.tryImport((Chunk) (Object) this, primer, skyLight);
        return skyLight;
    }

    @ModifyConstant(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/world/chunk/ChunkPrimer;II)V",
        constant = @Constant(intValue = 16), require = 2, expect = 2)
    private int nova$skipImportedLoop(final int limit) {
        return nova$primerImported ? 0 : limit;
    }
}
