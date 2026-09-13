package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.client.util.ClientLightingGuard;
import github.kasuminova.novaeng.client.util.LightingConstants;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ChunkCache.class)
public abstract class MixinChunkCacheLighting {

    @Shadow
    protected World world;

    @Shadow
    protected abstract int getLightForExt(EnumSkyBlock type, BlockPos pos);

    /**
     * @author circulation
     * @reason 区块烘焙热路径（RenderChunk.worldView），覆写直接返回常量，避免注入回调对象分配
     */
    @Overwrite
    public int getCombinedLight(final BlockPos pos, final int lightValue) {
        if (this.world.isRemote && ClientLightingGuard.isActive()) {
            return LightingConstants.PACKED_FULL_BRIGHT;
        }
        final int sky = this.getLightForExt(EnumSkyBlock.SKY, pos);
        int block = this.getLightForExt(EnumSkyBlock.BLOCK, pos);
        if (block < lightValue) {
            block = lightValue;
        }
        return sky << 20 | block << 4;
    }
}
