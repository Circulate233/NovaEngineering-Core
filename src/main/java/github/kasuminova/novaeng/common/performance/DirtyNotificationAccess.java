package github.kasuminova.novaeng.common.performance;

/**
 * Identifies a queue replay without suppressing subclass flags, chunk dirtiness or network updates.
 */
public interface DirtyNotificationAccess {
    DirtyNotificationState nova$notificationState();
}
