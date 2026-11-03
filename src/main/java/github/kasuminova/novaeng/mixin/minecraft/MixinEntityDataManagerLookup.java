package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ReportedException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

@Mixin(EntityDataManager.class)
public abstract class MixinEntityDataManagerLookup {
    @Unique
    private static final EntityDataManager.DataEntry<?>[] NOVA_EMPTY_ENTRIES = new EntityDataManager.DataEntry<?>[0];
    @Unique
    private EntityDataManager.DataEntry<?>[] nova$byId = NOVA_EMPTY_ENTRIES;
    @Shadow
    @Final
    private Map<Integer, EntityDataManager.DataEntry<?>> entries;
    @Shadow
    @Final
    private ReadWriteLock lock;

    @Redirect(method = "setEntry", at = @At(value = "INVOKE", remap = false,
        target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), require = 1)
    private Object nova$publishEntry(final Map<Integer, EntityDataManager.DataEntry<?>> map, final Object key, final Object value) {
        final int id = (Integer) key;
        final EntityDataManager.DataEntry<?> entry = (EntityDataManager.DataEntry<?>) value;
        final Object previous = map.put(id, entry);
        // The original write lock is held here. All readers retain the matching read lock.
        if (id >= 0 && id <= 254) {
            if (id >= nova$byId.length)
                nova$byId = Arrays.copyOf(nova$byId, Math.max(16, Integer.highestOneBit(id) << 1));
            nova$byId[id] = entry;
        }
        return previous;
    }

    /**
     * @author NovaEngineering
     * @reason Read legal IDs directly while preserving registration publication, live Entry values, and error reports.
     */
    @Overwrite
    @SuppressWarnings("unchecked")
    private <T> EntityDataManager.DataEntry<T> getEntry(final DataParameter<T> key) {
        lock.readLock().lock();
        try {
            final int id = key.getId();
            return (EntityDataManager.DataEntry<T>) (id >= 0 && id <= 254
                ? id < nova$byId.length ? nova$byId[id] : null : entries.get(id));
        } catch (final Throwable failure) {
            final CrashReport report = CrashReport.makeCrashReport(failure, "Getting synched entity data");
            final CrashReportCategory category = report.makeCategory("Synched entity data");
            category.addCrashSection("Data ID", key);
            throw new ReportedException(report);
        } finally {
            lock.readLock().unlock();
        }
    }
}
