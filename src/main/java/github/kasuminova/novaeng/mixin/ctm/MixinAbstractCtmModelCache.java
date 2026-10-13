package github.kasuminova.novaeng.mixin.ctm;

import com.google.common.cache.CacheBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;

import java.util.concurrent.TimeUnit;

@Mixin(value = AbstractCTMBakedModel.class, remap = false)
public abstract class MixinAbstractCtmModelCache {

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE",
        target = "Lcom/google/common/cache/CacheBuilder;expireAfterAccess(JLjava/util/concurrent/TimeUnit;)Lcom/google/common/cache/CacheBuilder;",
        ordinal = 1), remap = false, require = 1)
    private static CacheBuilder<?, ?> nova$keepModelCacheEntries(final CacheBuilder<?, ?> builder,
                                                                 final long duration,
                                                                 final TimeUnit unit) {
        return builder;
    }
}
