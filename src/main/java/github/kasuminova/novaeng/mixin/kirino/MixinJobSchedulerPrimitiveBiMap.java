package github.kasuminova.novaeng.mixin.kirino;

import com.cleanroommc.kirino.KirinoCommonCore;
import com.cleanroommc.kirino.ecs.entity.EntityManager;
import com.cleanroommc.kirino.ecs.entity.EntityQuery;
import com.cleanroommc.kirino.ecs.job.JobDataInjector;
import com.cleanroommc.kirino.ecs.job.JobDataQuery;
import com.cleanroommc.kirino.ecs.job.JobInstantiator;
import com.cleanroommc.kirino.ecs.job.JobRegistry;
import com.cleanroommc.kirino.ecs.job.JobScheduler;
import com.cleanroommc.kirino.ecs.job.JobScheduler.ExecutionHandle;
import com.cleanroommc.kirino.ecs.job.ParallelJob;
import com.cleanroommc.kirino.ecs.storage.ArchetypeDataPool;
import com.cleanroommc.kirino.ecs.storage.ArrayRange;
import com.google.common.base.Preconditions;

import github.kasuminova.novaeng.common.util.EntityIndexAccess;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(value = JobScheduler.class, remap = false)
public abstract class MixinJobSchedulerPrimitiveBiMap {
    @Shadow
    @Final
    private JobRegistry jobRegistry;

    @Unique
    private static int nova$entityId(ArchetypeDataPool pool, int index) {
        if (pool instanceof EntityIndexAccess primitive)
            return primitive.nova$requireEntityId(index);
        return pool.getEntityID(index).orElseThrow(IllegalStateException::new);
    }

    @Unique
    private static boolean nova$isDeprecated(ArrayRange range, int index) {
        if (range.deprecatedIndexes instanceof IntSet primitive) return primitive.contains(index);
        return range.deprecatedIndexes.contains(index);
    }

    @Shadow
    private ParallelJob newParallelJob(
        JobInstantiator instantiator,
        Map<JobDataQuery, JobDataInjector> dataQueries,
        Map<String, JobDataInjector> externalQueries,
        ArchetypeDataPool archetype,
        Map<String, Object> externalData) {
        throw new AssertionError();
    }

    /**
     * @author NovaEngineering
     * @reason Pass primitive entity IDs to sequential and worker jobs without Optional or Integer wrappers.
     */
    @Overwrite
    public ExecutionHandle executeParallelJob(
        EntityManager entityManager,
        Class<? extends ParallelJob> clazz,
        Map<String, Object> externalData,
        Executor executor) {
        Map<JobDataQuery, JobDataInjector> parallelJobDataQueries =
            this.jobRegistry.getParallelJobDataQueries(clazz);
        Map<String, JobDataInjector> parallelJobExternalDataQueries =
            this.jobRegistry.getParallelJobExternalDataQueries(clazz);
        JobInstantiator instantiator = this.jobRegistry.getParallelJobInstantiator(clazz);
        if (parallelJobDataQueries == null
            || parallelJobExternalDataQueries == null
            || instantiator == null) {
            throw new IllegalStateException(
                "Parallel job class " + clazz.getName() + " isn't registered.");
        }
        if (!parallelJobExternalDataQueries.isEmpty()) {
            Preconditions.checkArgument(
                externalData != null,
                "Argument \"externalData\" must not be null since there are %d external data"
                    + " queries.",
                parallelJobExternalDataQueries.size());
            for (String key : parallelJobExternalDataQueries.keySet()) {
                Preconditions.checkArgument(
                    externalData.containsKey(key),
                    "Missing the entry \"%s\" from \"externalData\".",
                    key);
            }
        }
        EntityQuery query = entityManager.newQuery();
        ((ParallelJob) instantiator.instantiate()).query(query);
        List<ArchetypeDataPool> archetypes = entityManager.startQuery(query);
        List<CompletableFuture<Void>> futures = new ObjectArrayList<>();
        int threadOrdinal = 0;
        for (ArchetypeDataPool archetype : archetypes) {
            ParallelJob job =
                this.newParallelJob(
                    instantiator,
                    parallelJobDataQueries,
                    parallelJobExternalDataQueries,
                    archetype,
                    externalData);
            ArrayRange arrayRange = archetype.getArrayRange();
            int workload = 0;
            for (int i = arrayRange.start; i < arrayRange.end; ++i) {
                if (nova$isDeprecated(arrayRange, i)) continue;
                int jobWorkload = job.estimateWorkload(i);
                Preconditions.checkState(
                    jobWorkload >= 1,
                    "The estimated workload at index=%s must be greater than or equal to 1."
                        + " (Parallel job class: %s)",
                    i,
                    clazz.getName());
                workload += jobWorkload;
            }
            int targetWorkloadPerThread =
                KirinoCommonCore.KIRINO_CONFIG_HUB.getTargetWorkloadPerThread();
            int futureCount = Math.ceilDivExact(workload, targetWorkloadPerThread);
            if (futureCount > Runtime.getRuntime().availableProcessors()) {
                futureCount = Runtime.getRuntime().availableProcessors();
                targetWorkloadPerThread = workload / futureCount;
            }
            if (futureCount <= 1) {
                for (int i = arrayRange.start; i < arrayRange.end; ++i) {
                    if (nova$isDeprecated(arrayRange, i)) continue;
                    int entityID = nova$entityId(archetype, i);
                    job.execute(entityManager, i, entityID, threadOrdinal);
                }
                ++threadOrdinal;
                continue;
            }
            int accumulated = 0;
            int startIndex = arrayRange.start;
            for (int i = arrayRange.start; i < arrayRange.end; ++i) {
                if (nova$isDeprecated(arrayRange, i)
                    || (accumulated += job.estimateWorkload(i)) < targetWorkloadPerThread)
                    continue;
                int endIndexExclusive = i + 1;
                int finalThreadOrdinal = threadOrdinal++;
                int finalStartIndex = startIndex;
                ParallelJob jobPerThread =
                    this.newParallelJob(
                        instantiator,
                        parallelJobDataQueries,
                        parallelJobExternalDataQueries,
                        archetype,
                        externalData);
                futures.add(
                    CompletableFuture.runAsync(
                        () -> {
                            for (int j = finalStartIndex; j < endIndexExclusive; ++j) {
                                if (nova$isDeprecated(arrayRange, j)) continue;
                                int entityID = nova$entityId(archetype, j);
                                jobPerThread.execute(
                                    entityManager, j, entityID, finalThreadOrdinal);
                            }
                        },
                        executor));
                startIndex = endIndexExclusive;
                accumulated = 0;
            }
            boolean hasRemainingWork = false;
            for (int j = startIndex; j < arrayRange.end; ++j) {
                if (nova$isDeprecated(arrayRange, j)) continue;
                hasRemainingWork = true;
                break;
            }
            if (!hasRemainingWork) continue;
            int finalThreadOrdinal = threadOrdinal++;
            int finalStartIndex = startIndex;
            ParallelJob jobPerThread =
                this.newParallelJob(
                    instantiator,
                    parallelJobDataQueries,
                    parallelJobExternalDataQueries,
                    archetype,
                    externalData);
            futures.add(
                CompletableFuture.runAsync(
                    () -> {
                        for (int j = finalStartIndex; j < arrayRange.end; ++j) {
                            if (nova$isDeprecated(arrayRange, j)) continue;
                            int entityID = nova$entityId(archetype, j);
                            jobPerThread.execute(
                                entityManager, j, entityID, finalThreadOrdinal);
                        }
                    },
                    executor));
        }
        if (futures.isEmpty()) {
            return new ExecutionHandle(
                CompletableFuture.completedFuture(null), threadOrdinal, false);
        }
        if (futures.size() == 1) {
            return new ExecutionHandle(futures.getFirst(), threadOrdinal, true);
        }
        return new ExecutionHandle(
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])),
            threadOrdinal,
            true);
    }
}
