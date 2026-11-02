package github.kasuminova.novaeng.mixin.minecraft;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.biome.BiomeCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeCache.class)
public abstract class MixinBiomeCacheLastEntry {
    @Unique
    private long nova$lastKey;
    @Unique
    private BiomeCache.Block nova$lastEntry;

    @Redirect(method = "getEntry", at = @At(value = "INVOKE", remap = false,
        target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"), require = 1)
    private Object nova$lastEntry(final Long2ObjectMap<BiomeCache.Block> map, final long key) {
        return nova$lastEntry != null && nova$lastKey == key ? nova$lastEntry : map.get(key);
    }

    @Inject(method = "getEntry", at = @At("RETURN"), require = 1)
    private void nova$rememberEntry(final int x, final int z, final CallbackInfoReturnable<BiomeCache.Block> cir) {
        nova$lastKey = (x >> 4) & 0xffffffffL | ((long) (z >> 4) << 32);
        nova$lastEntry = cir.getReturnValue();
    }

    @Inject(method = "cleanupCache", at = @At("HEAD"), require = 1)
    private void nova$invalidateEntry(final CallbackInfo ci) {
        nova$lastEntry = null;
    }
}
