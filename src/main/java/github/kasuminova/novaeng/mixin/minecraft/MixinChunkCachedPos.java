package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla allocates a fresh ChunkPos on every {@code getPos()} call. The coordinates are
 * written once in the constructor and ChunkPos is immutable, so the instance can be shared.
 */
@Mixin(Chunk.class)
public abstract class MixinChunkCachedPos {

    @Shadow
    @Final
    public int x;

    @Shadow
    @Final
    public int z;
    @Unique
    private volatile ChunkPos nova$cachedPos;

    @Inject(method = "<init>(Lnet/minecraft/world/World;II)V", at = @At("RETURN"))
    private void nova$buildCachedPos(final CallbackInfo ci) {
        this.nova$cachedPos = new ChunkPos(this.x, this.z);
    }

    @Redirect(method = "getPos()Lnet/minecraft/util/math/ChunkPos;",
        at = @At(value = "NEW", target = "net/minecraft/util/math/ChunkPos"), require = 1)
    private ChunkPos nova$reuseChunkPos(final int x, final int z) {
        ChunkPos cachedPos = this.nova$cachedPos;
        if (cachedPos == null) {
            this.nova$cachedPos = cachedPos = new ChunkPos(x, z);
        }
        return cachedPos;
    }

}
