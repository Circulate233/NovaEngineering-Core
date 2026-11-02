package github.kasuminova.novaeng.common.tile.ecotech.efabricator;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.storage.data.IAEItemStack;
import appeng.items.misc.ItemEncodedPattern;
import appeng.me.GridAccessException;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import com.glodblock.github.util.FluidCraftingPatternDetails;
import com.glodblock.github.util.FluidPatternDetails;
import github.kasuminova.mmce.common.util.PatternItemFilter;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.container.ContainerEFabricatorPatternSearch;
import github.kasuminova.novaeng.common.container.data.EFabricatorPatternData;
import github.kasuminova.novaeng.common.network.PktEFabricatorPatternSearchGUIUpdate;
import hellfirepvp.modularmachinery.ModularMachinery;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public final class EFabricatorPatternBus extends EFabricatorPart implements IAEAppEngInventory {

    public static final int PATTERN_SLOTS = 72;
    private final Set<IAEItemStack> aePatterns = new ObjectOpenHashSet<>();
    private final AppEngInternalInventory patterns = new AppEngInternalInventory(this, PATTERN_SLOTS, 1, PatternItemFilter.INSTANCE);
    private final ObjectArrayList<ICraftingPatternDetails> details = new ObjectArrayList<>(PATTERN_SLOTS);
    private int validPatterns;
    private ICraftingPatternDetails[] detailsSnapshot;

    public EFabricatorPatternBus() {
        this.details.size(PATTERN_SLOTS);
    }

    public Set<IAEItemStack> getAePatterns() {
        return this.aePatterns;
    }

    public AppEngInternalInventory getPatterns() {
        return this.patterns;
    }

    private void refreshPatterns() {
        for (int i = 0; i < PATTERN_SLOTS; i++) {
            this.refreshPattern(i);
        }
        this.notifyPatternChanged();
    }

    private void refreshPattern(final int slot) {
        this.detailsSnapshot = null;
        if (this.details.set(slot, null) != null) {
            this.validPatterns--;
        }
        final ItemStack pattern = this.patterns.getStackInSlot(slot);
        if (pattern.isEmpty() || !(pattern.getItem() instanceof ICraftingPatternItem item)) {
            return;
        }
        final var detail = item.getPatternForItem(pattern, this.getWorld());
        if (detail != null && (detail.isCraftable() || detail instanceof FluidCraftingPatternDetails)) {
            this.details.set(slot, detail);
            this.validPatterns++;
            this.detailsSnapshot = null;
        }
    }

    public List<ICraftingPatternDetails> getDetails() {
        final List<ICraftingPatternDetails> result = new ObjectArrayList<>(this.validPatterns);
        for (final var detail : this.details) {
            if (detail != null) {
                result.add(detail);
            }
        }
        return result;
    }

    public int getValidPatterns() {
        return this.validPatterns;
    }

    void provideCrafting(final ICraftingProvider provider, final ICraftingProviderHelper tracker) {
        ICraftingPatternDetails[] snapshot = this.detailsSnapshot;
        if (snapshot == null) {
            snapshot = new ICraftingPatternDetails[this.validPatterns];
            int index = 0;
            for (final ICraftingPatternDetails detail : this.details) {
                if (detail != null) {
                    snapshot[index++] = detail;
                }
            }
            this.detailsSnapshot = snapshot;
        }
        for (final ICraftingPatternDetails detail : snapshot) {
            if (detail.isCraftable() || detail instanceof FluidCraftingPatternDetails) {
                tracker.addCraftingOption(provider, detail);
            }
        }
    }

    @Override
    public void saveChanges() {
        this.markNoUpdateSync();
    }

    @Override
    public void onChangeInventory(final IItemHandler inventory, final int slot, final InvOperation operation,
                                  final ItemStack removed, final ItemStack added) {
        this.refreshPattern(slot);
        this.notifyPatternChanged();
        this.sendPatternSearchGUIUpdateToClient(slot);
        switch (operation) {
            case EXTRACT -> this.removePattern(removed);
            case INSERT -> this.addPattern(added);
            case SET -> {
                this.removePattern(removed);
                this.addPattern(added);
            }
        }
    }

    private void addPattern(final ItemStack stack) {
        if (stack.getItem() instanceof ItemEncodedPattern item) {
            final var pattern = item.getPatternForItem(stack, this.world);
            if (pattern != null && (pattern.isCraftable() || pattern instanceof FluidPatternDetails)) {
                this.aePatterns.add(pattern.getCondensedOutputs()[0]);
            }
        }
    }

    private void removePattern(final ItemStack stack) {
        if (stack.getItem() instanceof ItemEncodedPattern item) {
            final var pattern = item.getPatternForItem(stack, this.world);
            if (pattern != null && (pattern.isCraftable() || pattern instanceof FluidPatternDetails)) {
                this.aePatterns.remove(pattern.getCondensedOutputs()[0]);
            }
        }
    }

    private void notifyPatternChanged() {
        if (this.partController == null) {
            return;
        }
        try {
            final var channel = this.partController.getChannel();
            if (channel != null && channel.getProxy().isActive()) {
                channel.getProxy().getGrid().postEvent(new MENetworkCraftingPatternChange(channel, channel.getProxy().getNode()));
            }
        } catch (final GridAccessException ignored) {
        }
        this.partController.recalculateEnergyUsage();
    }

    private void sendPatternSearchGUIUpdateToClient(final int slot) {
        if (this.partController == null) {
            return;
        }
        final List<EntityPlayerMP> players = new ObjectArrayList<>();
        for (final EntityPlayer player : this.world.playerEntities) {
            if (player instanceof EntityPlayerMP serverPlayer
                && player.openContainer instanceof ContainerEFabricatorPatternSearch container
                && container.getOwner() == this.partController) {
                players.add(serverPlayer);
            }
        }
        if (players.isEmpty()) {
            return;
        }
        final var packet = new PktEFabricatorPatternSearchGUIUpdate(PktEFabricatorPatternSearchGUIUpdate.UpdateType.SINGLE,
            EFabricatorPatternData.of(new EFabricatorPatternData.PatternData(this.getPos(), slot, this.patterns.getStackInSlot(slot))));
        for (final var player : players) {
            NovaEngineeringCore.NET_CHANNEL.sendTo(packet, player);
        }
    }

    @Override
    public void validate() {
        super.validate();
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(this::refreshPatterns);
        }
    }

    @Override
    public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? (T) this.patterns
            : super.getCapability(capability, facing);
    }

    @Override
    public void readCustomNBT(final NBTTagCompound tag) {
        super.readCustomNBT(tag);
        this.patterns.readFromNBT(tag.getCompoundTag("patterns"));
        for (final ItemStack stack : this.patterns) {
            this.addPattern(stack);
        }
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        this.patterns.writeToNBT(tag, "patterns");
    }
}
