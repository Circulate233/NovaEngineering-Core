package github.kasuminova.novaeng.mixin.actinium;

import github.kasuminova.novaeng.client.entity.EntityChunkState;
import github.kasuminova.novaeng.client.entity.LoadedEntityChunkIndexProvider;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a client chunk to its world index after the first successful entity insertion.
 */
@Mixin(Chunk.class)
public abstract class MixinChunk implements EntityChunkState {

    @Shadow
    @Final
    private World world;

    @Shadow
    private boolean hasEntities;

    @Inject(method = "addEntity", at = @At("RETURN"), require = 1)
    private void nova$indexEntityChunk(final Entity entityIn, final CallbackInfo ci) {
        if (this.world instanceof LoadedEntityChunkIndexProvider provider) {
            provider.nova$getLoadedEntityChunkIndex().put((Chunk) (Object) this);
        }
    }

    /**
     * Exposes the exact vanilla field used by Actinium to skip never-populated chunks.
     *
     * @return current vanilla entity-presence state
     */
    @Override
    public boolean nova$hasEntities() {
        return this.hasEntities;
    }
}
