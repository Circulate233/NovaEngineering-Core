package github.kasuminova.novaeng.common.item.estorage;

import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class EItemStorageCell extends EStorageCell<IAEItemStack> {
    public EItemStorageCell(final int miloBytes) {
        super(miloBytes);
    }

    @Override
    public int getTotalTypes(@Nonnull final ItemStack cellItem) {
        return 9;
    }

    @Override
    public int getBytesPerType(@Nonnull final ItemStack cellItem) {
        return 1024;
    }

    @Override
    public double getIdleDrain() {
        return 0;
    }

    @Nonnull
    @Override
    public IStorageChannel<IAEItemStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
    }
}
