package github.kasuminova.novaeng.mixin.ae2;

import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.me.cache.GridStorageCache;
import appeng.me.cache.NetworkMonitor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = GridStorageCache.class, remap = false)
public class MixinGridStorageCache {

    @Shadow
    @Final
    private Map<IStorageChannel<? extends IAEStack<?>>, NetworkMonitor<?>> storageMonitors;

    @SuppressWarnings({"MixinAnnotationTarget", "rawtypes"})
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object fixNPE(Map<?, ?> instance, Object o) {
        var i = instance.get(o);
        if (instance == storageMonitors && i == null) {
            storageMonitors.put((IStorageChannel<?>) o, (NetworkMonitor) (i = new NetworkMonitor((GridStorageCache) (Object) this, (IStorageChannel<?>) o)));
        }
        return i;
    }
}