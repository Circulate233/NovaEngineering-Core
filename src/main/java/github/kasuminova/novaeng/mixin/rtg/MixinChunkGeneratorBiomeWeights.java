package github.kasuminova.novaeng.mixin.rtg;

import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rtg.world.gen.ChunkGeneratorRTG;

@Mixin(value = ChunkGeneratorRTG.class, remap = false)
public abstract class MixinChunkGeneratorBiomeWeights {

    @Unique
    private BlockPos.MutableBlockPos nova$biomeSamplePosition;

    @Redirect(method = "getNewerNoise(Lnet/minecraft/world/biome/BiomeProvider;IILrtg/world/gen/ChunkLandscape;)V",
        at = @At(value = "NEW", target = "net/minecraft/util/math/BlockPos"), require = 2)
    private BlockPos nova$reuseBiomeSamplePosition(final int x, final int y, final int z) {
        BlockPos.MutableBlockPos position = this.nova$biomeSamplePosition;
        if (position == null) {
            this.nova$biomeSamplePosition = position = new BlockPos.MutableBlockPos();
        }
        return position.setPos(x, y, z);
    }
}
