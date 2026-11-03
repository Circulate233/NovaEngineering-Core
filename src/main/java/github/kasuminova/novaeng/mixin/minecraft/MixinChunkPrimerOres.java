package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.PrimerOreChunk;
import github.kasuminova.novaeng.common.performance.PrimerOreGeneration;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chunk.class)
public abstract class MixinChunkPrimerOres implements PrimerOreChunk {

    @Unique
    private long[] nova$primerOreRules;

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/world/chunk/ChunkPrimer;II)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProvider;hasSkyLight()Z", unsafe = true))
    private void nova$generatePrimerOres(final World world, final ChunkPrimer primer, final int x, final int z,
                                         final CallbackInfo ci) {
        this.nova$primerOreRules = PrimerOreGeneration.generate(world, primer, x, z);
    }

    @Override
    public long[] nova$getPrimerOreRules() {
        return this.nova$primerOreRules;
    }

    @Override
    public void nova$setPrimerOreRules(final long[] rules) {
        this.nova$primerOreRules = rules;
    }
}
