package github.kasuminova.novaeng.common.tile;

import com.circulation.circulation_networks.api.EnergyAmount;
import com.circulation.circulation_networks.api.IEnergyHandler;
import com.circulation.circulation_networks.api.IMachineNodeBlockEntity;
import com.circulation.circulation_networks.api.node.IMachineNode;
import com.circulation.circulation_networks.api.node.NodeContext;
import com.circulation.circulation_networks.api.node.NodeType;
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

import java.math.BigDecimal;
import java.math.BigInteger;

public class TileDreamEnergyPort extends BaseNodeTileEntity<IMachineNode> implements IMachineNodeBlockEntity {

    private final IEnergyHandler energyHandler = new DreamenergyHandler();
    @Setter
    @Getter
    private BlockPos ctrlPos;

    public TileDreamEnergyPort() {

    }

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
            if (ctrl.getFoundMachine() != null && ctrl.getFoundMachine().getRegistryName().equals(DreamEnergyCore.REGISTRY_NAME)) {
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

    @SuppressWarnings("ReturnOfInnerClass")
    public final class DreamenergyHandler implements IEnergyHandler {

        private static final BigInteger max = BigDecimal.valueOf(1.0E256d).toBigInteger();
        private final EnergyAmount receive = EnergyAmount.obtain(0);
        private final EnergyAmount send = EnergyAmount.obtain(0);
        private final EnergyAmount canSend = EnergyAmount.obtain(0);
        private boolean init = false;

        @Override
        public IEnergyHandler init(TileEntity tileEntity, HubNode.HubMetadata hubMetadata) {
            if (getCtrlStructureFormed()) {
                canSend.init(DreamEnergyCore.getEnergyStoredString(getCtrl()));
                init = true;
            } else init = false;
            return this;
        }

        @Override
        public IEnergyHandler init(ItemStack itemStack, HubNode.HubMetadata hubMetadata) {
            return this;
        }

        @Override
        public void clear() {
            if (init) {
                var ctrl = getCtrl();
                DreamEnergyCore.extractEnergy(ctrl, send.asBigInteger());
                DreamEnergyCore.receiveEnergy(ctrl, receive.asBigInteger());
                canSend.setZero();
                receive.setZero();
                send.setZero();
                init = false;
            }
        }

        @Override
        public EnergyAmount receiveEnergy(EnergyAmount energyAmount, HubNode.HubMetadata hubMetadata) {
            receive.add(energyAmount);
            return energyAmount;
        }

        @Override
        public EnergyAmount extractEnergy(EnergyAmount energyAmount, HubNode.HubMetadata hubMetadata) {
            canSend.subtract(energyAmount);
            send.add(energyAmount);
            return EnergyAmount.obtain(energyAmount);
        }

        @Override
        public EnergyAmount canExtractValue(HubNode.HubMetadata hubMetadata) {
            return EnergyAmount.obtain(canSend);
        }

        @Override
        public EnergyAmount canReceiveValue(HubNode.HubMetadata hubMetadata) {
            return EnergyAmount.obtain(max);
        }

        @Override
        public boolean canExtract(IEnergyHandler iEnergyHandler, HubNode.HubMetadata hubMetadata) {
            if (!init) {
                return false;
            }
            return canSend.compareTo(0) > 0;
        }

        @Override
        public boolean canReceive(IEnergyHandler iEnergyHandler, HubNode.HubMetadata hubMetadata) {
            return init;
        }

        @Override
        public void recycle() {
            clear();
        }

        @Override
        public EnergyType getType(HubNode.HubMetadata hubMetadata) {
            if (init) {
                return EnergyType.STORAGE;
            }
            return EnergyType.INVALID;
        }
    }
}
