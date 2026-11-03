package github.kasuminova.novaeng.common.tile.ecotech.efabricator;

import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.util.inv.AppEngInternalInventory;
import ae2.util.inv.InternalInventoryHost;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public final class EFabricatorPatternBus extends EFabricatorPart implements InternalInventoryHost {

    public static final int PATTERN_SLOTS = 72;
    private final Set<AEKey> aePatterns = new ObjectOpenHashSet<>();
    private final AppEngInternalInventory patterns = new AppEngInternalInventory(this, PATTERN_SLOTS, 1);
    private final ObjectArrayList<IPatternDetails> details = new ObjectArrayList<>(PATTERN_SLOTS);
    private int validPatterns;

    public EFabricatorPatternBus() {
        this.details.size(PATTERN_SLOTS);
    }

    public Set<AEKey> getAePatterns() {
        return this.aePatterns;
    }

    public AppEngInternalInventory getPatterns() {
        return this.patterns;
    }

    private void refreshPatterns() {
        for (int i = 0; i < PATTERN_SLOTS; i++) {
            this.refreshPattern(i);
        }
        this.refreshPatternOutputs();
        this.notifyPatternChanged();
    }

    private void refreshPattern(final int slot) {
        details.set(slot, null);
        final ItemStack pattern = this.patterns.getStackInSlot(slot);
        if (pattern.isEmpty()) {
            return;
        }
        this.details.set(slot, PatternDetailsHelper.decodePattern(pattern, getWorld()));
    }

    public List<IPatternDetails> getDetails() {
        final List<IPatternDetails> result = new ObjectArrayList<>(this.validPatterns);
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

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        this.markNoUpdateSync();
    }

    @Override
    public void onChangeInventory(final AppEngInternalInventory inventory, final int slot) {
        this.refreshPattern(slot);
        this.sendPatternSearchGUIUpdateToClient(slot);
        this.refreshPatternOutputs();
        this.notifyPatternChanged();
    }

    @Override
    public boolean isClientSide() {
        return world != null && world.isRemote;
    }

    private void refreshPatternOutputs() {
        aePatterns.clear();
        for (IPatternDetails detail : details) {
            if (detail == null) {
                return;
            }
            final GenericStack stack = detail.getPrimaryOutput();
            if (stack == null) {
                return;
            }
            final AEKey key = stack.what();
            if (key == null) {
                return;
            }
            aePatterns.add(key);
        }
    }

    private void notifyPatternChanged() {
        if (this.partController == null) {
            return;
        }
        final var channel = this.partController.getChannel();
        if (channel == null) {
            return;
        }
        final var mainNode = channel.getMainNode();
        if (mainNode.isActive()) {
            ICraftingProvider.requestUpdate(mainNode);
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
        this.patterns.readFromNBT(tag, "patterns");
        refreshPatterns();
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        this.patterns.writeToNBT(tag, "patterns");
    }
}
