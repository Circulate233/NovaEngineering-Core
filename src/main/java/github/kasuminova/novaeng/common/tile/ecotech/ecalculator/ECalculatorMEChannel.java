package github.kasuminova.novaeng.common.tile.ecotech.ecalculator;

import ae2.api.AECapabilities;
import ae2.api.networking.GridFlags;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.events.GridCraftingCpuChange;
import ae2.api.networking.security.IActionSource;
import ae2.api.util.AECableType;
import ae2.me.ManagedGridNode;
import ae2.me.cluster.implementations.CraftingCPUCluster;
import ae2.me.helpers.IGridConnectedTile;
import ae2.me.helpers.MachineSource;
import github.kasuminova.novaeng.common.block.ecotech.ecalculator.BlockECalculatorMEChannel;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class ECalculatorMEChannel extends ECalculatorPart implements IGridConnectedTile {

    private static final IGridNodeListener<ECalculatorMEChannel> NODE_LISTENER = new IGridNodeListener<>() {
        @Override
        public void onSaveChanges(final ECalculatorMEChannel nodeOwner, final IGridNode node) {
            nodeOwner.saveChanges();
        }

        @Override
        public void onStateChanged(final ECalculatorMEChannel nodeOwner,
                                   final IGridNode node,
                                   final State state) {
            nodeOwner.onMainNodeStateChanged(state);
        }
    };

    protected IManagedGridNode mainNode;
    protected final IActionSource source = new MachineSource(this);

    private boolean wasActive = false;

    public ECalculatorMEChannel() {
        this.mainNode = createMainNode();
    }

    private IManagedGridNode createMainNode() {
        return new ManagedGridNode(this, NODE_LISTENER)
            .setIdlePowerUsage(1.0D)
            .setFlags(GridFlags.REQUIRE_CHANNEL, GridFlags.DENSE_CAPACITY)
            .setVisualRepresentation(getVisualItemStack())
            .setInWorldNode(true)
            .setTagName("channel");
    }

    public IActionSource getSource() {
        return source;
    }

    public ItemStack getVisualItemStack() {
        ECalculatorController controller = getController();
        return new ItemStack(Item.getItemFromBlock(controller == null ? BlockECalculatorMEChannel.INSTANCE : controller.getParentController()), 1, 0);
    }

    @Override
    public void onMainNodeStateChanged(final IGridNodeListener.State reason) {
        final boolean currentActive = this.mainNode.isActive();
        if (this.wasActive != currentActive) {
            this.wasActive = currentActive;
            postCPUClusterChangeEvent();
        }
    }

    protected void postCPUClusterChangeEvent() {
        if (this.mainNode.isActive()) {
            this.mainNode.ifPresent((grid, node) -> grid.postEvent(new GridCraftingCpuChange(node)));
        }
    }

    // Clusters

    public List<CraftingCPUCluster> getCPUs() {
        final boolean currentActive = this.mainNode.isActive();
        if (!currentActive || !isAssembled()) {
            return Collections.emptyList();
        }
        ECalculatorController controller = getController();
        if (controller == null) {
            return Collections.emptyList();
        }
        return controller.getClusterList();
    }

    // Misc

    @Nullable
    @Override
    public IGridNode getActionableNode() {
        return mainNode.getNode();
    }

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
        return AECableType.DENSE_SMART;
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


}
