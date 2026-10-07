package github.kasuminova.novaeng.common.performance;

import hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized;

/**
 * Queue call-site scopes, with no allocated callback or lambda per tile.
 */
public final class MMCENotifications {
    private MMCENotifications() {
    }

    public static void commit(final TileEntitySynchronized tile, final boolean fullUpdate) {
        final DirtyNotificationState state = ((DirtyNotificationAccess) tile).nova$notificationState();
        state.enterQueued();
        try {
            if (fullUpdate) {
                tile.markForUpdate();
            } else {
                tile.markNoUpdate();
            }
        } finally {
            state.leaveQueued();
        }
    }
}
