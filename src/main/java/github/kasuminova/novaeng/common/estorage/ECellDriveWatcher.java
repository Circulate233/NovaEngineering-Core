package github.kasuminova.novaeng.common.estorage;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.storage.MEStorage;
import ae2.me.storage.MEInventoryHandler;
import github.kasuminova.novaeng.common.tile.ecotech.estorage.EStorageCellDrive;

public class ECellDriveWatcher extends MEInventoryHandler {

    protected final EStorageCellDrive drive;

    public ECellDriveWatcher(final MEStorage i, final EStorageCellDrive drive) {
        super(i);
        this.drive = drive;
    }

    @Override
    public long insert(final AEKey input, final long amount, final Actionable type, final IActionSource src) {
        final long inserted = super.insert(input, amount, type, src);

        if (type == Actionable.MODULATE && inserted > 0) {
            this.drive.onWriting();
        }

        return inserted;
    }

    @Override
    public long extract(final AEKey request, final long amount, final Actionable type, final IActionSource src) {
        final long extractable = super.extract(request, amount, type, src);

        if (type == Actionable.MODULATE && extractable > 0) {
            this.drive.onWriting();
        }

        return extractable;
    }

}
