package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.common.performance.RegistryIdLookup;
import github.kasuminova.novaeng.common.util.RegistryIdLookupAccess;
import net.minecraftforge.registries.ForgeRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ForgeRegistry.class, remap = false)
public abstract class MixinForgeRegistryIdLookup implements RegistryIdLookupAccess {

    @Shadow
    boolean isFrozen;
    @Shadow
    @Final
    private BiMap<Integer, ?> ids;
    @Unique
    private volatile RegistryIdLookup nova$idLookup;
    @Unique
    private int nova$idReads;

    @Unique
    private volatile long nova$registryRevision;

    @Override
    public long nova$getRegistryRevision() {
        return nova$registryRevision;
    }

    @Override
    public Object nova$getIndexedId(final int id) {
        if (!NovaEngCoreConfig.CLIENT.optimizeFrozenRegistryIdLookup || !isFrozen || ids.getClass() != HashBiMap.class) {
            return ids.get(id);
        }
        RegistryIdLookup lookup = nova$idLookup;
        if (lookup == null) {
            // Amortize the snapshot over repeated lookups instead of adding a full
            // registry scan for a single iteration. This approximate counter only
            // controls cache construction; it is not used to validate cached data.
            if (nova$idReads < ids.size()) {
                nova$idReads++;
                return ids.get(id);
            }
            lookup = nova$buildIdLookup();
        }
        return lookup == null ? ids.get(id) : lookup.get(id, ids);
    }

    @Unique
    private synchronized RegistryIdLookup nova$buildIdLookup() {
        if (!isFrozen) {
            return null;
        }
        if (nova$idLookup == null) {
            nova$idLookup = RegistryIdLookup.create(ids);
        }
        return nova$idLookup;
    }

    @Unique
    private synchronized void nova$invalidateIdLookup() {
        nova$registryRevision++;
        nova$idReads = 0;
        nova$idLookup = null;
    }

    @Inject(method = {"freeze", "unfreeze"}, at = @At("HEAD"), require = 1)
    private void nova$onFreezeTransition(final CallbackInfo ci) {
        nova$invalidateIdLookup();
    }

    @Redirect(method = "add(ILnet/minecraftforge/registries/IForgeRegistryEntry;Ljava/lang/String;)I",
        at = @At(value = "INVOKE", target = "Lcom/google/common/collect/BiMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), require = 1)
    private Object nova$invalidateAfterPut(final BiMap<Object, Object> map, final Object key, final Object value) {
        try {
            return map.put(key, value);
        } finally {
            nova$invalidateIdLookup();
        }
    }

    @Redirect(method = {"clear", "sync"}, at = @At(value = "INVOKE", target = "Lcom/google/common/collect/BiMap;clear()V"), require = 1)
    private void nova$invalidateAfterClear(final BiMap<?, ?> map) {
        try {
            map.clear();
        } finally {
            nova$invalidateIdLookup();
        }
    }

    @Redirect(method = {"remove", "markDummy"}, at = @At(value = "INVOKE",
        target = "Lcom/google/common/collect/BiMap;remove(Ljava/lang/Object;)Ljava/lang/Object;"), require = 1)
    private Object nova$invalidateAfterRemove(final BiMap<?, ?> map, final Object key) {
        try {
            return map.remove(key);
        } finally {
            // markDummy can remove an ID before a later add fails on a frozen registry.
            nova$invalidateIdLookup();
        }
    }

    @Redirect(method = {"getValue(I)Lnet/minecraftforge/registries/IForgeRegistryEntry;",
        "getRaw(I)Lnet/minecraftforge/registries/IForgeRegistryEntry;"},
        at = @At(value = "INVOKE", target = "Lcom/google/common/collect/BiMap;get(Ljava/lang/Object;)Ljava/lang/Object;"), require = 1)
    private Object nova$lookupId(final BiMap<?, ?> map, final Object key) {
        return nova$getIndexedId((Integer) key);
    }
}
