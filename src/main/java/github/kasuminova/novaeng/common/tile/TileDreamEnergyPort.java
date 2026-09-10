package github.kasuminova.novaeng.common.tile;

import com.circulation.circulation_networks.api.EnergyAmount;
import com.circulation.circulation_networks.api.EnergyAmounts;
import com.circulation.circulation_networks.api.HandlerTickResult;
import com.circulation.circulation_networks.api.IEnergyHandler;
import com.circulation.circulation_networks.api.IMachineNodeBlockEntity;
import com.circulation.circulation_networks.api.node.IMachineNode;
import com.circulation.circulation_networks.api.node.NodeContext;
import com.circulation.circulation_networks.api.node.NodeType;
import com.circulation.circulation_networks.manager.HandlerBindingPolicy;
import com.circulation.circulation_networks.manager.HandlerInvalidationSink;
import com.circulation.circulation_networks.network.nodes.HubNode;
import com.circulation.circulation_networks.network.nodes.Node;
import com.circulation.circulation_networks.tiles.nodes.BaseNodeTileEntity;
import github.kasuminova.novaeng.common.block.BlockDreamEnergyPort;
import github.kasuminova.novaeng.common.machine.DreamEnergyCore;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public class TileDreamEnergyPort extends BaseNodeTileEntity<IMachineNode> implements IMachineNodeBlockEntity {

    private final IEnergyHandler energyHandler = new DreamenergyHandler();
    @Setter
    @Getter
    private BlockPos ctrlPos;

    @Override
    public @NotNull NodeType<DreamNode> getNodeType() {
        return BlockDreamEnergyPort.TYPE;
    }

    @Override
    public @NotNull IEnergyHandler getEnergyHandler() {
        return energyHandler;
    }

    @NotNull
    @Override
    public NBTTagCompound writeToNBT(@NotNull NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (this.ctrlPos != null) {
            compound.setLong("ctrlPos", this.ctrlPos.toLong());
        }
        return compound;
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("ctrlPos")) {
            this.ctrlPos = BlockPos.fromLong(compound.getLong("ctrlPos"));
        }
    }

    public TileMultiblockMachineController getCtrl() {
        if (ctrlPos == null || this.world == null) {
            return null;
        }
        if (this.world.getTileEntity(ctrlPos) instanceof TileMultiblockMachineController ctrl) {
            if (ctrl.getFoundMachine() != null && ctrl.getFoundMachine().getRegistryName().getPath().equals(DreamEnergyCore.REGISTRY_NAME.getPath())) {
                return ctrl;
            }
        }
        return null;
    }

    public boolean getCtrlStructureFormed() {
        var ctrl = getCtrl();
        if (ctrl != null) {
            return ctrl.isStructureFormed();
        }
        return false;
    }

    public static final class DreamNode extends Node implements IMachineNode {

        public DreamNode(NBTTagCompound nbt) {
            super(BlockDreamEnergyPort.TYPE, nbt);
        }

        public DreamNode(NodeContext context) {
            super(BlockDreamEnergyPort.TYPE, context, 0);
        }

        @Override
        public IEnergyHandler.EnergyType getType() {
            return IEnergyHandler.EnergyType.STORAGE;
        }

        @Override
        public double getEnergyScope() {
            return 0;
        }

        @Override
        public double getEnergyScopeSq() {
            return 0;
        }
    }

    public final class DreamenergyHandler implements IEnergyHandler {

        private static final BigInteger MAX_RECEIVE = BigDecimal.valueOf(Double.MAX_VALUE).toBigInteger();

        private static final HandlerBindingPolicy BINDING_POLICY = HandlerBindingPolicy.of(
            HandlerBindingPolicy.TickLifecycle.BEGIN_END_TICK,
            HandlerBindingPolicy.RoleScope.RUNTIME_DYNAMIC,
            HandlerBindingPolicy.MappingScope.NONE,
            HandlerBindingPolicy.PairMatching.NONE
        );

        private final EnergyAmount received = EnergyAmount.obtain(0L);
        private final EnergyAmount extracted = EnergyAmount.obtain(0L);
        private final EnergyAmount available = EnergyAmount.obtain(0L);

        private boolean blockEntityBound;
        private boolean transferActive;
        private boolean advertisedTransferActive;
        private long activeEpoch = Long.MIN_VALUE;

        @Override
        public HandlerBindingPolicy bindingPolicy() {
            return BINDING_POLICY;
        }

        @Override
        public void bindBlockEntity(TileEntity tileEntity, HandlerInvalidationSink invalidationSink) {
            if (blockEntityBound) {
                throw new IllegalStateException("Dreamenergy handler is already bound");
            }

            Objects.requireNonNull(tileEntity, "tileEntity");
            Objects.requireNonNull(invalidationSink, "invalidationSink");
            blockEntityBound = true;
            transferActive = false;
            advertisedTransferActive = false;
        }

        @Override
        public HandlerTickResult beginServerTick(long epoch) {
            if (!blockEntityBound) {
                throw new IllegalStateException("Dreamenergy handler is not bound");
            }
            if (activeEpoch != Long.MIN_VALUE) {
                throw new IllegalStateException(
                    "Dreamenergy handler tick is already active for epoch " + activeEpoch
                );
            }

            activeEpoch = epoch;
            received.setZero();
            extracted.setZero();
            available.setZero();

            transferActive = getCtrlStructureFormed();
            if (transferActive) {
                available.init(DreamEnergyCore.getEnergyStoredString(getCtrl()));
            }

            if (advertisedTransferActive != transferActive) {
                advertisedTransferActive = transferActive;
                return HandlerTickResult.STATE_CHANGED;
            }
            return HandlerTickResult.UNCHANGED;
        }

        @Override
        public void endServerTick(long epoch) {
            if (activeEpoch != epoch) {
                throw new IllegalStateException(
                    "Dreamenergy handler tick epoch mismatch: expected " + activeEpoch + ", got " + epoch
                );
            }

            try {
                if (!transferActive) {
                    return;
                }

                var controller = getCtrl();
                if (extracted.isPositive()) {
                    DreamEnergyCore.extractEnergy(controller, extracted.asBigInteger());
                }
                if (received.isPositive()) {
                    DreamEnergyCore.receiveEnergy(controller, received.asBigInteger());
                }
            } finally {
                received.setZero();
                extracted.setZero();
                available.setZero();
                transferActive = false;
                activeEpoch = Long.MIN_VALUE;
            }
        }

        @Override
        public void unbindBlockEntity() {
            received.setZero();
            extracted.setZero();
            available.setZero();
            transferActive = false;
            advertisedTransferActive = false;
            activeEpoch = Long.MIN_VALUE;
            blockEntityBound = false;
        }

        @Override
        public void bindItem(ItemStack itemStack, @Nullable HubNode.HubMetadata hubMetadata) {
            throw new UnsupportedOperationException("Dreamenergy does not support item energy bindings");
        }

        @Override
        public void unbindItem() {
            // This handler never acquires item-binding state.
        }

        @Override
        public EnergyAmount receiveEnergy(EnergyAmount maxReceive, @Nullable HubNode.HubMetadata hubMetadata) {
            if (!transferActive || !maxReceive.isPositive()) {
                return EnergyAmounts.ZERO;
            }

            received.add(maxReceive);
            return EnergyAmount.obtain(maxReceive);
        }

        @Override
        public EnergyAmount extractEnergy(EnergyAmount maxExtract, @Nullable HubNode.HubMetadata hubMetadata) {
            if (!transferActive || !maxExtract.isPositive() || !available.isPositive()) {
                return EnergyAmounts.ZERO;
            }

            EnergyAmount transferred;
            if (maxExtract.compareTo(available) >= 0) {
                transferred = EnergyAmount.obtain(available);
                available.setZero();
            } else {
                transferred = EnergyAmount.obtain(maxExtract);
                available.subtract(transferred);
            }

            extracted.add(transferred);
            return transferred;
        }

        @Override
        public EnergyAmount canExtractValue(@Nullable HubNode.HubMetadata hubMetadata) {
            return transferActive ? EnergyAmount.obtain(available) : EnergyAmounts.ZERO;
        }

        @Override
        public EnergyAmount canReceiveValue(@Nullable HubNode.HubMetadata hubMetadata) {
            return transferActive ? EnergyAmount.obtain(MAX_RECEIVE) : EnergyAmounts.ZERO;
        }

        @Override
        public boolean canExtract(IEnergyHandler receiveHandler, @Nullable HubNode.HubMetadata hubMetadata) {
            return transferActive && available.isPositive();
        }

        @Override
        public boolean canReceive(IEnergyHandler sendHandler, @Nullable HubNode.HubMetadata hubMetadata) {
            return transferActive;
        }

        @Override
        public EnergyType getType(@Nullable HubNode.HubMetadata hubMetadata) {
            return transferActive ? EnergyType.STORAGE : EnergyType.INVALID;
        }
    }
}
