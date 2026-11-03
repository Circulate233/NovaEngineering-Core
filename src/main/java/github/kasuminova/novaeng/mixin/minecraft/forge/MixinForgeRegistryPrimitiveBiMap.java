package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.Multimap;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.common.performance.RegistryIdLookup;
import github.kasuminova.novaeng.common.util.RegistryAvailabilityAccess;
import github.kasuminova.novaeng.common.util.RegistryIdLookupAccess;
import github.kasuminova.novaeng.common.util.collection.Int2ObjectBiMap;
import github.kasuminova.novaeng.common.util.collection.Int2ObjectHashBiMap;
import github.kasuminova.novaeng.common.util.collection.PrimitiveBiMaps;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.ForgeRegistry.OverrideOwner;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.IForgeRegistryInternal;
import net.minecraftforge.registries.RegistryDelegate;
import net.minecraftforge.registries.RegistryManager;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;

/**
 * Primitive numeric storage and explicit lookup hooks.
 */
@Mixin(value = ForgeRegistry.class, remap = false, priority = 1000)
public abstract class MixinForgeRegistryPrimitiveBiMap<V extends IForgeRegistryEntry<V>>
    implements RegistryIdLookupAccess, RegistryAvailabilityAccess {
    @Shadow
    boolean isFrozen;
    @Shadow
    @Final
    @Mutable
    private BiMap<Integer, V> ids;
    @Unique
    private volatile RegistryIdLookup nova$idLookup;
    @Unique
    private int nova$idReads;
    @Shadow
    private V defaultValue;
    @Shadow
    @Final
    private RegistryManager stage;
    @Shadow
    @Final
    private BiMap<ResourceLocation, V> names;
    @Shadow
    @Final
    private Class<V> superType;
    @Shadow
    @Final
    private Map<ResourceLocation, ResourceLocation> aliases;
    @Shadow
    @Final
    private ResourceLocation defaultKey;
    @Shadow
    @Final
    private IForgeRegistry.AddCallback<V> add;
    @Shadow
    @Final
    private BitSet availabilityMap;
    @Shadow
    @Final
    private Set<ResourceLocation> dummies;
    @Shadow
    @Final
    private Set<Integer> blocked;
    @Shadow
    @Final
    private Multimap<ResourceLocation, V> overrides;
    @Shadow
    @Final
    private BiMap<OverrideOwner, V> owners;
    @Shadow
    @Final
    private IForgeRegistry.DummyFactory<V> dummyFactory;
    @Shadow
    @Final
    private boolean isDelegated;
    @Shadow
    @Final
    private int min;
    @Shadow
    @Final
    private int max;
    @Shadow
    @Final
    private boolean allowOverrides;
    @Shadow
    @Final
    private boolean isModifiable;

    @Override
    public Object nova$getIndexedId(final int id) {
        if (NovaEngCoreConfig.CLIENT.optimizeFrozenRegistryIdLookup && isFrozen) {
            final long revision = PrimitiveBiMaps.modificationVersion(ids);
            if (revision >= 0) {
                RegistryIdLookup lookup = nova$idLookup;
                if (lookup != null && lookup.revision() == revision) return lookup.get(id, ids);
                if (nova$idReads < ids.size()) nova$idReads++;
                else {
                    lookup = nova$buildIdLookup(revision);
                    if (lookup != null) return lookup.get(id, ids);
                }
            }
        }
        return PrimitiveBiMaps.getIntKey(ids, id);
    }

    @Unique
    private synchronized RegistryIdLookup nova$buildIdLookup(final long revision) {
        if (!isFrozen || PrimitiveBiMaps.modificationVersion(ids) != revision) return null;
        if (nova$idLookup != null && nova$idLookup.revision() == revision) return nova$idLookup;
        final RegistryIdLookup result = RegistryIdLookup.create(ids, revision);
        if (result == null || !isFrozen || PrimitiveBiMaps.modificationVersion(ids) != revision)
            return null;
        nova$idLookup = result;
        return result;
    }

    @Override
    public long nova$getRegistryRevision() {
        return PrimitiveBiMaps.modificationVersion(ids);
    }

    /**
     * @author NovaEngineering
     * @reason Preserve the default entry while reading primitive IDs.
     */
    @Overwrite
    public int getID(final V value) {
        if (ids instanceof Int2ObjectBiMap<?> primitive) {
            final int id = primitive.inverse().getIntOrDefault(value, -1);
            return id != -1 || defaultValue == null
                ? id
                : primitive.inverse().getIntOrDefault(defaultValue, -1);
        }
        Integer id = ids.inverse().get(value);
        if (id == null && defaultValue != null) id = ids.inverse().get(defaultValue);
        return id == null ? -1 : id.intValue();
    }

    /**
     * @author NovaEngineering
     * @reason Read a primitive ID without applying the default entry.
     */
    @Overwrite
    private int getIDRaw(final V value) {
        if (ids instanceof Int2ObjectBiMap<?> primitive)
            return primitive.inverse().getIntOrDefault(value, -1);
        final Integer id = ids.inverse().get(value);
        return id == null ? -1 : id.intValue();
    }

    @Redirect(
        method = "<init>",
        at =
        @At(
            value = "FIELD",
            target =
                "Lnet/minecraftforge/registries/ForgeRegistry;ids:Lcom/google/common/collect/BiMap;",
            opcode = Opcodes.PUTFIELD),
        require = 1)
    private void nova$primitiveIds(final ForgeRegistry<V> owner, final BiMap<Integer, V> original) {
        ids = Int2ObjectHashBiMap.copyOf(original);
    }

    /**
     * @author NovaEngineering
     * @reason Keep the frozen lookup cache and primitive ID path.
     */
    @Overwrite
    public V getValue(final int id) {
        final Object value = nova$getIndexedId(id);
        return value == null ? defaultValue : (V) value;
    }

    /**
     * @author NovaEngineering
     * @reason Resolve raw IDs without boxing.
     */
    @Overwrite
    public V getRaw(final int id) {
        return (V) nova$getIndexedId(id);
    }

    @Shadow
    private V getRaw(ResourceLocation key) {
        throw new AssertionError();
    }

    @Shadow
    private RegistryDelegate<V> getDelegate(V value) {
        throw new AssertionError();
    }

    @Shadow
    public abstract ResourceLocation getKey(V value);

    @Shadow
    public abstract boolean isLocked();

    @Shadow
    public abstract boolean containsKey(ResourceLocation key);

    @Shadow
    abstract int add(int id, V value);

    @Shadow
    abstract Map<ResourceLocation, String> getOverrideOwners();

    @Override
    public int nova$nextAvailableRegistryId(int from) {
        return availabilityMap.nextSetBit(from);
    }

    /**
     * @author NovaEngineering
     * @reason Maintain registration semantics using primitive ID lookup and mutation.
     */
    @Overwrite
    int add(int id, V value, String owner) {
        final var primitive = (Int2ObjectBiMap<V>) ids;
        ResourceLocation key = value == null ? null : value.getRegistryName();
        Preconditions.checkNotNull(
            key, "Can't use a null-name for the registry, object %s.", value);
        Preconditions.checkNotNull(value, "Can't add null-object to the registry, name %s.", key);
        int idToUse = id;
        if (idToUse < 0 || this.availabilityMap.get(idToUse)) {
            idToUse = this.availabilityMap.nextClearBit(this.min);
        }
        if (idToUse > this.max) {
            throw new RuntimeException(
                String.format("Invalid id %d - maximum id range exceeded.", idToUse));
        }
        V oldEntry = this.getRaw(key);
        if (oldEntry == value) {
            FMLLog.bigWarning(
                "Registry {}: The object {} has been registered twice for the same name {}.",
                this.superType.getSimpleName(),
                value,
                key);
            return this.getID(value);
        }
        if (oldEntry != null) {
            if (!this.allowOverrides) {
                throw new IllegalArgumentException(
                    String.format(
                        "The name %s has been registered twice, for %s and %s.",
                        key, this.getRaw(key), value));
            }
            if (owner == null) {
                throw new IllegalStateException(
                    String.format(
                        "Could not determine owner for the override on %s. Value: %s",
                        key, value));
            }
            if (ForgeRegistry.DEBUG) {
                FMLLog.log.debug(
                    "Registry {} Override: {} {} -> {}",
                    this.superType.getSimpleName(),
                    key,
                    oldEntry,
                    value);
            }
            idToUse = this.getID(oldEntry);
        }
        final int foundId = primitive.inverse().getIntOrDefault(value, -1);
        if (foundId != -1) {
            V otherThing = primitive.get(foundId);
            throw new IllegalArgumentException(
                String.format(
                    "The object %s{%x} has been registered twice, using the names %s and"
                        + " %s. (Other object at this id is %s{%x})",
                    value,
                    System.identityHashCode(value),
                    this.getKey(value),
                    key,
                    otherThing,
                    System.identityHashCode(otherThing)));
        }
        if (this.isLocked()) {
            throw new IllegalStateException(
                String.format("The object %s (name %s) is being added too late.", value, key));
        }
        if (this.defaultKey != null && this.defaultKey.equals(key)) {
            if (this.defaultValue != null) {
                throw new IllegalStateException(
                    String.format(
                        "Attemped to override already set default value. This is not"
                            + " allowed: The object %s (name %s)",
                        value, key));
            }
            this.defaultValue = value;
        }
        this.names.put(key, value);
        primitive.putDiscard(idToUse, value, false);
        this.availabilityMap.set(idToUse);
        this.owners.put(new OverrideOwner(owner == null ? key.getNamespace() : owner, key), value);
        if (this.isDelegated) {
            this.getDelegate(value).setName(key);
            if (oldEntry != null) {
                if (!this.overrides.get(key).contains(oldEntry)) {
                    this.overrides.put(key, oldEntry);
                }
                this.overrides.get(key).remove(value);
                if (this.stage == RegistryManager.ACTIVE) {
                    this.getDelegate(oldEntry).changeReference(value);
                }
            }
        }
        if (this.add != null) {
            this.add.onAdd(
                (IForgeRegistryInternal<V>) this,
                this.stage,
                idToUse,
                value,
                oldEntry);
        }
        if (this.dummies.remove(key) && ForgeRegistry.DEBUG) {
            FMLLog.log.debug("Registry {} Dummy Remove: {}", this.superType.getSimpleName(), key);
        }
        if (ForgeRegistry.DEBUG) {
            FMLLog.log.trace(
                "Registry {} add: {} {} {} (req. id {})",
                this.superType.getSimpleName(),
                key,
                idToUse,
                value,
                id);
        }
        return idToUse;
    }

    /**
     * @author NovaEngineering
     * @reason Maintain registration semantics using primitive ID lookup and mutation.
     */
    @Overwrite
    public V remove(ResourceLocation key) {
        if (!this.isModifiable) {
            throw new UnsupportedOperationException(
                "Attempted to remove from a non-modifiable Forge Registry");
        }
        if (this.isLocked()) {
            throw new IllegalStateException("Attempted to remove from the registry to late.");
        }
        V value = this.names.remove(key);
        if (value != null) {
            final var inverse = ((Int2ObjectBiMap<V>) ids).inverse();
            final boolean present = inverse.containsKey(value);
            final int id = inverse.removeInt(value);
            if (!present) {
                throw new IllegalStateException(
                    "Removed a entry that did not have an associated id: "
                        + key
                        + " "
                        + value
                        + " This should never happen unless hackery!");
            }
            if (ForgeRegistry.DEBUG) {
                FMLLog.log.trace(
                    "Registry {} remove: {} {}", this.superType.getSimpleName(), key, id);
            }
        }
        return value;
    }

    /**
     * @author NovaEngineering
     * @reason Maintain registration semantics using primitive ID lookup and mutation.
     */
    @Overwrite
    boolean markDummy(ResourceLocation key, int id) {
        int realId;
        if (this.dummyFactory == null) {
            return false;
        }
        V dummy = this.dummyFactory.createDummy(key);
        if (ForgeRegistry.DEBUG) {
            FMLLog.log.debug("Registry Dummy Add: {} {} -> {}", key, id, dummy);
        }
        this.availabilityMap.clear(id);
        if (this.containsKey(key)) {
            V value = this.names.remove(key);
            if (value == null) {
                throw new IllegalStateException(
                    "ContainsKey for "
                        + key
                        + " was true, but removing by name returned no value.. This should"
                        + " never happen unless hackery!");
            }
            final var inverse = ((Int2ObjectBiMap<V>) ids).inverse();
            final boolean present = inverse.containsKey(value);
            final int oldid = inverse.removeInt(value);
            if (!present) {
                throw new IllegalStateException(
                    "Removed a entry that did not have an associated id: "
                        + key
                        + " "
                        + value
                        + " This should never happen unless hackery!");
            }
            if (oldid != id) {
                FMLLog.log.debug(
                    "Registry {}: Dummy ID mismatch {} {} -> {}",
                    this.superType.getSimpleName(),
                    key,
                    oldid,
                    id);
            }
            if (ForgeRegistry.DEBUG) {
                FMLLog.log.debug(
                    "Registry {} remove: {} {}", this.superType.getSimpleName(), key, oldid);
            }
        }
        if ((realId = this.add(id, dummy)) != id) {
            FMLLog.log.warn(
                "Registry {}: Object did not get ID it asked for. Name: {} Expected: {} Got:"
                    + " {}",
                this.superType.getSimpleName(),
                key,
                id,
                realId);
        }
        this.dummies.add(key);
        return true;
    }

    /**
     * @author NovaEngineering
     * @reason Copy registry IDs through native entries without a boxed BiConsumer.
     */
    @Overwrite
    public ForgeRegistry.Snapshot makeSnapshot() {
        final ForgeRegistry.Snapshot snapshot = new ForgeRegistry.Snapshot();
        for (Int2ObjectMap.Entry<V> entry : ((Int2ObjectBiMap<V>) ids).int2ObjectEntrySet()) {
            PrimitiveBiMaps.putObjectInt(snapshot.ids, getKey(entry.getValue()), entry.getIntKey());
        }
        snapshot.aliases.putAll(aliases);
        ((IntSet) snapshot.blocked).addAll((IntSet) blocked);
        snapshot.dummied.addAll(dummies);
        snapshot.overrides.putAll(getOverrideOwners());
        return snapshot;
    }
}
