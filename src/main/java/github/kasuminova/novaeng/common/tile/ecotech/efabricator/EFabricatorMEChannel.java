package github.kasuminova.novaeng.common.tile.ecotech.efabricator;

import ae2.api.AECapabilities;
import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.networking.GridFlags;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.networking.security.IActionSource;
import ae2.api.util.AECableType;
import ae2.me.ManagedGridNode;
import ae2.me.helpers.IGridConnectedTile;
import ae2.me.helpers.MachineSource;
import com.google.common.primitives.Ints;
import github.kasuminova.novaeng.common.block.ecotech.efabricator.BlockEFabricatorMEChannel;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorWorker.CraftWork;
import hellfirepvp.modularmachinery.ModularMachinery;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class EFabricatorMEChannel extends EFabricatorPart implements ICraftingProvider, IGridConnectedTile {

    private final IGridNodeListener<EFabricatorMEChannel> nodeListener = new IGridNodeListener<>() {
        @Override
        public void onSaveChanges(EFabricatorMEChannel nodeOwner, IGridNode node) {
            nodeOwner.saveChanges();
        }

        @Override
        public void onStateChanged(EFabricatorMEChannel nodeOwner, IGridNode node, State state) {
            nodeOwner.postPatternChangeEvent();
        }
    };
    private final ItemStack visualItemStack;
    private final IManagedGridNode mainNode = new ManagedGridNode(this, nodeListener);
    private final IActionSource source = new MachineSource(this);
    private boolean wasActive;

    public EFabricatorMEChannel() {
        var controller = getController();
        visualItemStack = controller == null ? new ItemStack(BlockEFabricatorMEChannel.INSTANCE) : new ItemStack(Objects.requireNonNull(controller.getParentController()));
        mainNode.setIdlePowerUsage(1.0)
            .setFlags(GridFlags.REQUIRE_CHANNEL, GridFlags.DENSE_CAPACITY)
            .setVisualRepresentation(this.visualItemStack)
            .setInWorldNode(true)
            .setTagName("channel");
    }

    private static ItemStack getContainerItem(@Nullable final ItemStack stack) {
        if (stack == null) {
            return ItemStack.EMPTY;
        }
        final var item = stack.getItem();
        if (item != null && item.hasContainerItem(stack)) {
            ItemStack container = item.getContainerItem(stack);
            if (!container.isEmpty() && container.isItemStackDamageable() && container.getItemDamage() == container.getMaxDamage()) {
                container = ItemStack.EMPTY;
            }
            container.setCount(stack.getCount());
            return container;
        }
        if (!stack.isEmpty()) {
            stack.setCount(0);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    public IActionSource getSource() {
        return this.source;
    }

    public ItemStack getVisualItemStack() {
        final EFabricatorController controller = this.getController();
        return controller == null ? new ItemStack(BlockEFabricatorMEChannel.INSTANCE)
            : new ItemStack(Objects.requireNonNull(controller.getParentController()));
    }

    private void postPatternChangeEvent() {
        final boolean active = this.mainNode.isActive();
        if (this.wasActive != active) {
            this.wasActive = active;
            ICraftingProvider.requestUpdate(mainNode);
        }
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        final EFabricatorController controller = this.getController();
        if (controller == null) {
            return Collections.emptyList();
        }

        final var patternBuses = controller.getPatternBuses();
        if (patternBuses == null) {
            return Collections.emptyList();
        }

        final List<IPatternDetails> details = new ObjectArrayList<>();
        for (final var bus : controller.getPatternBuses()) {
            details.addAll(bus.getDetails());
        }
        return details;
    }

    @Override
    public boolean pushPattern(final IPatternDetails pattern, final KeyCounter[] inputHolder, final int multiplier) {
        if (this.isBusy()) {
            return false;
        }

        if (!(pattern.getPrimaryOutput().what() instanceof AEItemKey outputKey)) {
            return false;
        }
        if (inputHolder.length > 9 || multiplier <= 0) {
            return false;
        }

        final ItemStack[] remaining = new ItemStack[9];
        Arrays.fill(remaining, ItemStack.EMPTY);
        for (int i = 0; i < inputHolder.length; i++) {
            final var entry = inputHolder[i].getFirstEntry();
            if (entry == null) {
                return false;
            }
            final long value = entry.getLongValue();
            if (!(entry.getKey() instanceof AEItemKey key)) {
                return false;
            }
            remaining[i] = getContainerItem(key.toStack(Ints.saturatedCast(value)));
        }
        final var outputAmount = Math.multiplyExact(pattern.getPrimaryOutput().amount(), multiplier);
        if (outputAmount > Integer.MAX_VALUE) {
            return false;
        }
        return this.partController.offerWork(new CraftWork(remaining, outputKey.toStack(Ints.saturatedCast(outputAmount)), multiplier));
    }

    public boolean insertPattern(final ItemStack stack) {
        if (!PatternDetailsHelper.isEncodedPattern(stack)) {
            return false;
        }
        if (partController != null) {
            return partController.insertPattern(stack);
        }
        return false;
    }

    @Override
    public boolean isBusy() {
        return this.partController == null || this.partController.isQueueFull();
    }

    @Override
    @Nonnull
    public IManagedGridNode getMainNode() {
        return this.mainNode;
    }

    @Override
    public void readCustomNBT(final NBTTagCompound tag) {
        super.readCustomNBT(tag);
        this.mainNode.loadFromNBT(tag);
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        this.mainNode.saveToNBT(tag);
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        this.mainNode.destroy();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        this.mainNode.destroy();
    }

    @Override
    public void onAssembled() {
        super.onAssembled();
        this.mainNode.setVisualRepresentation(this.getVisualItemStack());
        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            if (!mainNode.isReady() && !getWorld().isRemote) {
                this.mainNode.create(getWorld(), getPos());
            }
            this.partController.recalculateEnergyUsage();
        });
    }

    @Override
    public void onDisassembled() {
        super.onDisassembled();
        this.mainNode.setVisualRepresentation(this.getVisualItemStack());
        this.mainNode.destroy();
    }

    @Override
    public void saveChanges() {
        this.markDirty();
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        return AECableType.DENSE_SMART;
    }

    @Override
    public boolean hasCapability(@NotNull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == AECapabilities.IN_WORLD_GRID_NODE_HOST || super.hasCapability(capability, facing);
    }

    @Override
    public @Nullable <T> T getCapability(@NotNull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == AECapabilities.IN_WORLD_GRID_NODE_HOST) {
            return AECapabilities.IN_WORLD_GRID_NODE_HOST.cast(this);
        }
        return super.getCapability(capability, facing);
    }
}
