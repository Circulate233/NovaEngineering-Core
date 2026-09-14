package github.kasuminova.novaeng.common.tile.ecotech.estorage;

import ae2.api.AECapabilities;
import ae2.api.config.AccessRestriction;
import ae2.api.config.Actionable;
import ae2.api.config.PowerMultiplier;
import ae2.api.networking.GridFlags;
import ae2.api.networking.GridHelper;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.events.GridPowerStorageStateChanged;
import ae2.api.networking.energy.IAEPowerStorage;
import ae2.api.networking.security.IActionSource;
import ae2.api.storage.IStorageMounts;
import ae2.api.storage.IStorageProvider;
import ae2.api.storage.MEStorage;
import ae2.api.storage.MEStorageChangeListener;
import ae2.api.storage.MEStorageMonitor;
import ae2.api.storage.cells.StorageCell;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.AEKeyType;
import ae2.api.stacks.AEKeyTypes;
import ae2.api.stacks.KeyCounter;
import ae2.api.util.AECableType;
import ae2.me.helpers.IGridConnectedTile;
import ae2.me.helpers.MachineSource;
import github.kasuminova.novaeng.common.estorage.ECellDriveWatcher;
import github.kasuminova.novaeng.common.block.ecotech.efabricator.BlockEFabricatorMEChannel;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EStorageMEChannel extends EStoragePart implements IGridConnectedTile, IStorageProvider, IAEPowerStorage {

    private static final IGridNodeListener<EStorageMEChannel> NODE_LISTENER = new IGridNodeListener<>() {
        @Override
        public void onSaveChanges(final EStorageMEChannel nodeOwner, final IGridNode node) {
            nodeOwner.saveChanges();
        }

        @Override
        public void onStateChanged(final EStorageMEChannel nodeOwner,
                                   final IGridNode node,
                                   final State state) {
            nodeOwner.onMainNodeStateChanged(state);
        }
    };

    protected IManagedGridNode mainNode = createMainNode();
    protected final IActionSource source = new MachineSource(this);

    protected int priority = 0;
    private boolean wasActive = false;

    private IManagedGridNode createMainNode() {
        return GridHelper.createManagedNode(this, NODE_LISTENER)
            .setIdlePowerUsage(1.0D)
            .setFlags(GridFlags.REQUIRE_CHANNEL, GridFlags.DENSE_CAPACITY)
            .setVisualRepresentation(getVisualItemStack())
            .setInWorldNode(true)
            .setTagName("channel")
            .addService(IStorageProvider.class, this)
            .addService(IAEPowerStorage.class, this);
    }

    public IActionSource getSource() {
        return source;
    }

    @Override
    public void mountInventories(final IStorageMounts mounts) {
        if (partController == null) {
            return;
        }

        for (final EStorageCellDrive drive : partController.getCellDrives()) {
            for (final AEKeyType keyType : AEKeyTypes.getAll()) {
                mountDrive(mounts, drive, keyType);
            }
        }
    }

    private void mountDrive(final IStorageMounts mounts,
                            final EStorageCellDrive drive,
                            final AEKeyType keyType) {
        final ECellDriveWatcher handler = drive.getHandler(keyType);
        if (handler == null) {
            return;
        }

        final StorageCell cell = drive.getCellHandler();
        if (cell == null) {
            throw new IllegalStateException("Storage drive returned a handler without a cell");
        }
        mounts.mount(new DriveStorageMonitor(handler, cell), priority);
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public ItemStack getVisualItemStack() {
        final EStorageController controller = getController();
        return new ItemStack(Item.getItemFromBlock(
            controller == null ? BlockEFabricatorMEChannel.INSTANCE : controller.getParentController()), 1, 0);
    }

    @Override
    public void onMainNodeStateChanged(final IGridNodeListener.State reason) {
        final boolean currentActive = mainNode.isActive();
        if (wasActive != currentActive) {
            wasActive = currentActive;
            IStorageProvider.requestUpdate(mainNode);
        }
    }

    @Override
    public double injectAEPower(final double amt, @Nonnull final Actionable mode) {
        if (partController == null || amt < 0.000001D) {
            return 0;
        }
        if (mode == Actionable.MODULATE && getAECurrentPower() < 0.01D && amt > 0) {
            postPowerStorageStateChanged(GridPowerStorageStateChanged.PowerEventType.PROVIDE_POWER);
        }
        return partController.injectPower(amt, mode);
    }

    @Override
    public double extractAEPower(final double amt,
                                 @Nonnull final Actionable mode,
                                 @Nonnull final PowerMultiplier multiplier) {
        if (partController == null) {
            return 0;
        }
        if (mode == Actionable.MODULATE
            && getAECurrentPower() >= getAEMaxPower() - 0.001D
            && amt > 0) {
            postPowerStorageStateChanged(GridPowerStorageStateChanged.PowerEventType.RECEIVE_POWER);
        }
        return multiplier.divide(partController.extractPower(multiplier.multiply(amt), mode));
    }

    private void postPowerStorageStateChanged(final GridPowerStorageStateChanged.PowerEventType type) {
        final IGridNode node = mainNode.getNode();
        if (node != null) {
            node.grid().postEvent(new GridPowerStorageStateChanged(this, type));
        }
    }

    @Override
    public double getAEMaxPower() {
        return partController == null ? 0 : partController.getMaxEnergyStore();
    }

    @Override
    public double getAECurrentPower() {
        return partController == null ? 0 : partController.getEnergyStored();
    }

    @Override
    public boolean isAEPublicPowerStorage() {
        return true;
    }

    @Nonnull
    @Override
    public AccessRestriction getPowerFlow() {
        return AccessRestriction.READ_WRITE;
    }

    @Nullable
    @Override
    public IGridNode getActionableNode() {
        return mainNode.getNode();
    }

    @Override
    public IManagedGridNode getMainNode() {
        return mainNode;
    }

    @Nullable
    @Override
    public IGridNode getGridNode(@Nonnull final EnumFacing dir) {
        return mainNode.getNode();
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull final EnumFacing dir) {
        return AECableType.DENSE_SMART;
    }

    @Override
    public void saveChanges() {
        markDirty();
    }

    public void securityBreak() {
        getWorld().destroyBlock(getPos(), true);
    }

    @Override
    public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
        if (capability == AECapabilities.IN_WORLD_GRID_NODE_HOST) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == AECapabilities.IN_WORLD_GRID_NODE_HOST) {
            return AECapabilities.IN_WORLD_GRID_NODE_HOST.cast(this);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readCustomNBT(final NBTTagCompound compound) {
        super.readCustomNBT(compound);
        mainNode.loadFromNBT(compound);
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        mainNode.saveToNBT(compound);
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        mainNode.destroy();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        mainNode.destroy();
    }

    @Override
    public void onAssembled() {
        super.onAssembled();
        mainNode.setVisualRepresentation(getVisualItemStack());
        final IManagedGridNode node = mainNode;
        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            if (mainNode == node && !node.isReady() && !getWorld().isRemote) {
                node.create(getWorld(), getPos());
            }
            partController.recalculateEnergyUsage();
        });
    }

    @Override
    public void onDisassembled() {
        super.onDisassembled();
        int ownerId = -1;
        if (mainNode.isReady()) {
            final IGridNode node = mainNode.getNode();
            if (node != null) {
                ownerId = node.getOwningPlayerId();
            }
        }
        mainNode.destroy();
        mainNode = createMainNode();
        if (ownerId >= 0) {
            mainNode.setOwningPlayerId(ownerId);
        }
        wasActive = false;
    }

    private static final class DriveStorageMonitor implements MEStorageMonitor {
        private final MEStorage handler;
        private final StorageCell monitor;

        private DriveStorageMonitor(final MEStorage handler, final StorageCell monitor) {
            this.handler = handler;
            this.monitor = monitor;
        }

        @Override
        public void addListener(final MEStorageChangeListener listener, final Object verificationToken) {
            monitor.addListener(listener, verificationToken);
        }

        @Override
        public void removeListener(final MEStorageChangeListener listener) {
            monitor.removeListener(listener);
        }

        @Override
        public boolean isPreferredStorageFor(final AEKey what, final IActionSource source) {
            return handler.isPreferredStorageFor(what, source);
        }

        @Override
        public boolean isStickyStorageFor(final AEKey what, final IActionSource source) {
            return handler.isStickyStorageFor(what, source);
        }

        @Override
        public long insert(final AEKey what, final long amount, final Actionable mode, final IActionSource source) {
            return handler.insert(what, amount, mode, source);
        }

        @Override
        public long extract(final AEKey what, final long amount, final Actionable mode, final IActionSource source) {
            return handler.extract(what, amount, mode, source);
        }

        @Override
        public void getAvailableStacks(final KeyCounter out) {
            handler.getAvailableStacks(out);
        }

        @Override
        public KeyCounter getAvailableStacks() {
            return handler.getAvailableStacks();
        }

        @Override
        public net.minecraft.util.text.ITextComponent getDescription() {
            return handler.getDescription();
        }
    }
}
