package github.kasuminova.novaeng.mixin.kirino;

import com.cleanroommc.kirino.ecs.component.CleanComponent;
import com.cleanroommc.kirino.ecs.component.ComponentRegistry;
import com.cleanroommc.kirino.ecs.component.schema.def.field.scalar.FlattenedScalarType;
import com.cleanroommc.kirino.ecs.storage.ArchetypeDataPool;
import com.cleanroommc.kirino.ecs.storage.ArrayRange;
import com.cleanroommc.kirino.ecs.storage.HeapPool;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;

import github.kasuminova.novaeng.common.util.EntityIndexAccess;
import github.kasuminova.novaeng.common.util.collection.Int2IntBiMap;
import github.kasuminova.novaeng.common.util.collection.Int2IntHashBiMap;
import github.kasuminova.novaeng.common.util.collection.PrimitiveBiMaps;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import org.jspecify.annotations.NonNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Mixin(value = HeapPool.class, remap = false)
public abstract class MixinHeapPoolPrimitiveBiMap extends ArchetypeDataPool
    implements EntityIndexAccess {
    @Shadow
    @Final
    private List<int[]> intPool;
    @Shadow
    @Final
    private List<float[]> floatPool;
    @Shadow
    @Final
    private List<boolean[]> booleanPool;
    @Shadow
    @Final
    private Map<Class<? extends CleanComponent>, HeapPool.ComDataLocation> componentDataLocations;
    @Shadow
    @Final
    @Mutable
    private List<Integer> freeIndexes;
    @Shadow
    private int indexCounter;
    @Shadow
    @Final
    @Mutable
    private BiMap<Integer, Integer> entityDataIndexes;

    protected MixinHeapPoolPrimitiveBiMap(
        ComponentRegistry registry,
        List<Class<? extends CleanComponent>> components,
        int initSize,
        int growStep,
        int shrinkStep) {
        super(registry, components, initSize, growStep, shrinkStep);
    }

    @Override
    public int nova$requireEntityId(final int index) {
        final Int2IntBiMap inverse = ((Int2IntBiMap) entityDataIndexes).inverse();
        Preconditions.checkState(inverse.containsKey(index));
        return inverse.getRequired(index);
    }

    /**
     * @author NovaEngineering
     * @reason Keep free entity indexes primitive when constructing the job's range.
     */
    @Overwrite
    public ArrayRange getArrayRange() {
        return InvokerArrayRange.nova$createRange(
            0, indexCounter, new IntOpenHashSet((IntList) freeIndexes));
    }

    @Redirect(
        method = "<init>",
        at =
        @At(
            value = "FIELD",
            target =
                "Lcom/cleanroommc/kirino/ecs/storage/HeapPool;freeIndexes:Ljava/util/List;",
            opcode = Opcodes.PUTFIELD),
        require = 1)
    private void nova$primitiveFreeIndexes(HeapPool owner, List<Integer> original) {
        freeIndexes = new IntArrayList();
    }

    @Redirect(
        method = {"<init>"},
        at =
        @At(
            value = "FIELD",
            target =
                "Lcom/cleanroommc/kirino/ecs/storage/HeapPool;entityDataIndexes:Lcom/google/common/collect/BiMap;",
            opcode = Opcodes.PUTFIELD),
        require = 1)
    private void nova$primitiveTable(final HeapPool owner, final BiMap<Integer, Integer> original) {
        final Int2IntHashBiMap replacement = new Int2IntHashBiMap(16);
        replacement.putAll(original);
        entityDataIndexes = replacement;
    }

    /**
     * @author NovaEngineering
     * @reason Test entity IDs without boxing.
     */
    @Overwrite
    public boolean containsEntity(final int entityID) {
        return PrimitiveBiMaps.containsIntKey(entityDataIndexes, entityID);
    }

    /**
     * @author NovaEngineering
     * @reason Access entity indexes through primitive operations while retaining pool behavior.
     */
    @Overwrite
    public @NonNull CleanComponent getComponent(int entityID, Class<? extends CleanComponent> component) {
        HeapPool.ComDataLocation location = this.componentDataLocations.get(component);
        int index = ((Int2IntBiMap) entityDataIndexes).getRequired(entityID);
        String comName = this.componentRegistry.getComponentName(component);
        int unitCount = this.componentRegistry.getComponentDescFlattened(comName).getUnitCount();
        int argIndex = 0;
        Object[] args = new Object[unitCount];
        int intArrIndex = location.intArrFrom;
        int floatArrIndex = location.floatArrFrom;
        int booleanArrIndex = location.booleanArrFrom;
        for (FlattenedScalarType flattenedScalarType : location.order) {
            if (flattenedScalarType == FlattenedScalarType.INT) {
                args[argIndex] = this.intPool.get(intArrIndex)[index];
                ++intArrIndex;
            } else if (flattenedScalarType == FlattenedScalarType.FLOAT) {
                args[argIndex] = Float.valueOf(this.floatPool.get(floatArrIndex)[index]);
                ++floatArrIndex;
            } else if (flattenedScalarType == FlattenedScalarType.BOOL) {
                args[argIndex] = this.booleanPool.get(booleanArrIndex)[index];
                ++booleanArrIndex;
            }
            ++argIndex;
        }
        return this.componentRegistry.newComponent(comName, args);
    }

    /**
     * @author NovaEngineering
     * @reason Access entity indexes through primitive operations while retaining pool behavior.
     */
    @Overwrite
    public void setComponent(int entityID, CleanComponent component) {
        Object[] args = this.componentRegistry.flattenComponent(component);
        HeapPool.ComDataLocation location = this.componentDataLocations.get(component.getClass());
        int index = ((Int2IntBiMap) entityDataIndexes).getRequired(entityID);
        int argIndex = 0;
        int intArrIndex = location.intArrFrom;
        int floatArrIndex = location.floatArrFrom;
        int booleanArrIndex = location.booleanArrFrom;
        for (FlattenedScalarType flattenedScalarType : location.order) {
            if (flattenedScalarType == FlattenedScalarType.INT) {
                this.intPool.get(intArrIndex)[index] = (Integer) args[argIndex];
                ++intArrIndex;
            } else if (flattenedScalarType == FlattenedScalarType.FLOAT) {
                this.floatPool.get(floatArrIndex)[index] = ((Float) args[argIndex]).floatValue();
                ++floatArrIndex;
            } else if (flattenedScalarType == FlattenedScalarType.BOOL) {
                this.booleanPool.get(booleanArrIndex)[index] = (Boolean) args[argIndex];
                ++booleanArrIndex;
            }
            ++argIndex;
        }
    }

    /**
     * @author NovaEngineering
     * @reason Access entity indexes through primitive operations while retaining pool behavior.
     */
    @Overwrite
    public void addEntity(int entityID, List<CleanComponent> components) {
        int index;
        if (this.freeIndexes.isEmpty()) {
            if (this.indexCounter >= this.currentSize) {
                this.currentSize += this.growStep;
                this.intPool.replaceAll(original -> Arrays.copyOf(original, this.currentSize));
                this.floatPool.replaceAll(original -> Arrays.copyOf(original, this.currentSize));
                this.booleanPool.replaceAll(original -> Arrays.copyOf(original, this.currentSize));
            }
            index = this.indexCounter++;
        } else {
            index = ((IntList) freeIndexes).removeInt(freeIndexes.size() - 1);
        }
        ((Int2IntBiMap) entityDataIndexes).putDiscard(entityID, index, false);
        for (Class<? extends CleanComponent> clazz : this.components) {
            CleanComponent component =
                Objects.requireNonNull(
                    components.stream()
                              .filter(c -> c.getClass().equals(clazz))
                              .findFirst()
                              .orElse(null));
            Object[] args = this.componentRegistry.flattenComponent(component);
            HeapPool.ComDataLocation location = this.componentDataLocations.get(clazz);
            int argIndex = 0;
            int intArrIndex = location.intArrFrom;
            int floatArrIndex = location.floatArrFrom;
            int booleanArrIndex = location.booleanArrFrom;
            for (FlattenedScalarType flattenedScalarType : location.order) {
                if (flattenedScalarType == FlattenedScalarType.INT) {
                    this.intPool.get(intArrIndex)[index] = (Integer) args[argIndex];
                    ++intArrIndex;
                } else if (flattenedScalarType == FlattenedScalarType.FLOAT) {
                    this.floatPool.get(floatArrIndex)[index] =
                        ((Float) args[argIndex]).floatValue();
                    ++floatArrIndex;
                } else if (flattenedScalarType == FlattenedScalarType.BOOL) {
                    this.booleanPool.get(booleanArrIndex)[index] = (Boolean) args[argIndex];
                    ++booleanArrIndex;
                }
                ++argIndex;
            }
        }
    }

    /**
     * @author NovaEngineering
     * @reason Access entity indexes through primitive operations while retaining pool behavior.
     */
    @Overwrite
    public void removeEntity(int entityID) {
        int index = ((Int2IntBiMap) entityDataIndexes).removeRequired(entityID);
        if (index == this.indexCounter - 1) {
            --this.indexCounter;
            IntList freeIndexesToRemove = new IntArrayList();
            while (((IntList) freeIndexes).contains(this.indexCounter - 1)) {
                freeIndexesToRemove.add(--this.indexCounter);
            }
            for (IntIterator it = freeIndexesToRemove.iterator(); it.hasNext(); ) {
                ((IntList) freeIndexes).rem(it.nextInt());
            }
            if (this.indexCounter + this.shrinkStep <= this.currentSize
                && this.currentSize - this.shrinkStep >= this.initSize) {
                this.currentSize -= this.shrinkStep;
                this.intPool.replaceAll(original -> Arrays.copyOf(original, this.currentSize));
                this.floatPool.replaceAll(original -> Arrays.copyOf(original, this.currentSize));
                this.booleanPool.replaceAll(original -> Arrays.copyOf(original, this.currentSize));
            }
        } else {
            ((IntList) freeIndexes).add(index);
        }
    }

    /**
     * @author NovaEngineering
     * @reason Access entity indexes through primitive operations while retaining pool behavior.
     */
    @Overwrite
    public Optional<Integer> getEntityID(int index) {
        return Optional.ofNullable(((Int2IntBiMap) entityDataIndexes).inverse().getBoxed(index));
    }
}
