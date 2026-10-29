package github.kasuminova.novaeng.common.tile.ecotech.efabricator;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.me.helpers.MachineSource;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import com.glodblock.github.util.FluidCraftingPatternDetails;
import github.kasuminova.mmce.common.util.PatternItemFilter;
import github.kasuminova.novaeng.common.block.ecotech.efabricator.BlockEFabricatorMEChannel;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorWorker.CraftWork;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

public final class EFabricatorMEChannel extends EFabricatorPart implements ICraftingProvider, IActionHost, IGridProxyable {

    private final AENetworkProxy aEProxy = new AENetworkProxy(this, "channel", this.getVisualItemStack(), true);
    private final IActionSource source = new MachineSource(this);
    private boolean wasActive;

    public EFabricatorMEChannel() {
        this.aEProxy.setIdlePowerUsage(1.0);
        this.aEProxy.setFlags(GridFlags.REQUIRE_CHANNEL, GridFlags.DENSE_CAPACITY);
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

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkPowerStatusChange event) {
        this.postPatternChangeEvent();
    }

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkChannelsChanged event) {
        this.postPatternChangeEvent();
    }

    private void postPatternChangeEvent() {
        final boolean active = this.aEProxy.isActive();
        if (this.wasActive != active) {
            this.wasActive = active;
            try {
                this.aEProxy.getGrid().postEvent(new MENetworkCraftingPatternChange(this, this.aEProxy.getNode()));
            } catch (final GridAccessException ignored) {
            }
        }
    }

    @Override
    public void provideCrafting(final ICraftingProviderHelper tracker) {
        final EFabricatorController controller = this.getController();
        if (controller == null) {
            return;
        }
        for (final var bus : controller.getPatternBuses()) {
            bus.provideCrafting(this, tracker);
        }
    }

    @Override
    public boolean pushPattern(final ICraftingPatternDetails pattern, final InventoryCrafting table) {
        if (this.isBusy()) {
            return false;
        }
        if (!pattern.isCraftable()) {
            return pattern instanceof FluidCraftingPatternDetails fluid && this.pushFluidPattern(fluid, table);
        }
        final ItemStack output = pattern.getOutput(table, this.getWorld());
        if (output.isEmpty()) {
            return false;
        }
        final ItemStack[] remaining = new ItemStack[9];
        Arrays.fill(remaining, ItemStack.EMPTY);
        int size = 0;
        for (int i = 0; i < Math.min(table.getSizeInventory(), 9); i++) {
            final ItemStack item = table.getStackInSlot(i);
            if (!item.isEmpty()) {
                if (size == 0) {
                    size = item.getCount();
                }
                remaining[i] = getContainerItem(item);
            }
        }
        output.setCount(output.getCount() * size);
        return this.partController.offerWork(new CraftWork(remaining, output, size));
    }

    private boolean pushFluidPattern(final FluidCraftingPatternDetails pattern, final InventoryCrafting table) {
        final var outputs = pattern.getOutputs();
        final ItemStack output = outputs[0] == null ? ItemStack.EMPTY : outputs[0].getCachedItemStack(outputs[0].getStackSize());
        if (output.isEmpty()) {
            return false;
        }
        final ItemStack[] remaining = new ItemStack[9];
        Arrays.fill(remaining, ItemStack.EMPTY);
        int size = 0;
        for (int i = 0; i < Math.min(table.getSizeInventory(), 9); i++) {
            final ItemStack item = table.getStackInSlot(i);
            if (!item.isEmpty()) {
                if (size == 0) {
                    size = item.getCount();
                    if (item.getItem() instanceof ItemFluidPacket) {
                        final int amount = ((FluidStack) FakeItemRegister.getStack(item)).amount;
                        final int patternAmount = ((FluidStack) FakeItemRegister.getStack(pattern.getInputs()[i])).amount;
                        size = amount / patternAmount;
                    }
                }
                remaining[i] = getContainerItem(item);
            }
        }
        output.setCount(output.getCount() * size);
        return this.partController.offerWork(new CraftWork(remaining, output, size));
    }

    public boolean insertPattern(final ItemStack stack) {
        return PatternItemFilter.INSTANCE.allowInsert(null, -1, stack)
            && this.partController != null && this.partController.insertPattern(stack);
    }

    @Override
    public boolean isBusy() {
        return this.partController == null || this.partController.isQueueFull();
    }

    @Override
    @Nonnull
    public IGridNode getActionableNode() {
        return this.aEProxy.getNode();
    }

    @Override
    @Nonnull
    public AENetworkProxy getProxy() {
        return this.aEProxy;
    }

    @Override
    @Nonnull
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Override
    public void gridChanged() {
    }

    @Override
    @Nullable
    public IGridNode getGridNode(@Nonnull final AEPartLocation direction) {
        return this.aEProxy.getNode();
    }

    @Override
    @Nonnull
    public AECableType getCableConnectionType(@Nonnull final AEPartLocation direction) {
        return AECableType.DENSE_SMART;
    }

    @Override
    public void securityBreak() {
        this.getWorld().destroyBlock(this.getPos(), true);
    }

    @Override
    public void readCustomNBT(final NBTTagCompound tag) {
        super.readCustomNBT(tag);
        this.aEProxy.readFromNBT(tag);
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        this.aEProxy.writeToNBT(tag);
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        this.aEProxy.onChunkUnload();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        this.aEProxy.invalidate();
    }

    @Override
    public void onAssembled() {
        super.onAssembled();
        this.aEProxy.setVisualRepresentation(this.getVisualItemStack());
        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            this.aEProxy.onReady();
            this.partController.recalculateEnergyUsage();
        });
    }

    @Override
    public void onDisassembled() {
        super.onDisassembled();
        this.aEProxy.setVisualRepresentation(this.getVisualItemStack());
        this.aEProxy.invalidate();
    }
}
