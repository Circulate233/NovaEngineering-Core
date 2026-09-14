package github.kasuminova.novaeng.common.tile;

import ae2.api.AECapabilities;
import ae2.api.networking.GridFlags;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.security.IActionSource;
import ae2.api.util.AECableType;
import ae2.me.ManagedGridNode;
import ae2.me.helpers.IGridConnectedTile;
import ae2.me.helpers.MachineSource;
import ae2.util.Platform;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class TileCustomControllerME extends TileCustomController implements IGridConnectedTile {
    private static final IGridNodeListener<TileCustomControllerME> NODE_LISTENER = new IGridNodeListener<>() {
        @Override
        public void onSaveChanges(final TileCustomControllerME nodeOwner, final IGridNode node) {
            nodeOwner.saveChanges();
        }

        @Override
        public void onStateChanged(final TileCustomControllerME nodeOwner,
                                   final IGridNode node,
                                   final State state) {
            nodeOwner.onMainNodeStateChanged(state);
        }
    };

    protected final IManagedGridNode mainNode = new ManagedGridNode(this, NODE_LISTENER)
        .setVisualRepresentation(getVisualItemStack())
        .setInWorldNode(true)
        .setTagName("aeProxy");
    protected final IActionSource source;

    public TileCustomControllerME() {
        this.source = new MachineSource(this);
        this.mainNode.setIdlePowerUsage(0.0D);
        this.mainNode.setFlags(GridFlags.REQUIRE_CHANNEL);
    }

    public abstract ItemStack getVisualItemStack();

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
    public void onMainNodeStateChanged(final IGridNodeListener.State reason) {
        notifyNeighbors();
    }

    private void notifyNeighbors() {
        if (!mainNode.isActive()) {
            return;
        }
        mainNode.ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        Platform.notifyBlocksOfNeighbors(this.getWorld(), this.getPos());
    }

    @Override
    @Nullable
    public IGridNode getActionableNode() {
        return mainNode.getNode();
    }

    @Nonnull
    @Override
    public IManagedGridNode getMainNode() {
        return mainNode;
    }

    @Override
    public void saveChanges() {
        markDirty();
    }

    @Nullable
    @Override
    public IGridNode getGridNode(@Nonnull final EnumFacing dir) {
        return mainNode.getNode();
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull final EnumFacing dir) {
        return AECableType.SMART;
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
    public void validate() {
        super.validate();
        if (!getWorld().isRemote) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
                if (!mainNode.isReady()) {
                    mainNode.create(getWorld(), getPos());
                }
            });
        }
    }
}
