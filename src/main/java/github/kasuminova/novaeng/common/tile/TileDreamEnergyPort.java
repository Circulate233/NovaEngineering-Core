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
    protected @NotNull NodeType<DreamNode> getNodeType() {
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

    public final class DreamenergyHandler implements IEnergyHandler {

        private static final BigInteger max = BigDecimal.valueOf(1.0E256d).toBigInteger();

        @Override
        public IEnergyHandler init(TileEntity tileEntity, HubNode.HubMetadata hubMetadata) {
            return this;
        }

        @Override
        public IEnergyHandler init(ItemStack itemStack, HubNode.HubMetadata hubMetadata) {
            return this;
        }

        @Override
        public void clear() {

        }

        @Override
        public EnergyAmount receiveEnergy(EnergyAmount energyAmount, HubNode.HubMetadata hubMetadata) {
            DreamEnergyCore.receiveEnergy(getCtrl(), energyAmount.asBigInteger());
            return energyAmount;
        }

        @Override
        public EnergyAmount extractEnergy(EnergyAmount energyAmount, HubNode.HubMetadata hubMetadata) {
            var ctrl = getCtrl();
            DreamEnergyCore.extractEnergy(ctrl, energyAmount.asBigInteger());
            return EnergyAmount.obtain(DreamEnergyCore.getEnergyStoredString(ctrl)).min(energyAmount);
        }

        @Override
        public EnergyAmount canExtractValue(HubNode.HubMetadata hubMetadata) {
            return EnergyAmount.obtain(DreamEnergyCore.getEnergyStoredString(getCtrl()));
        }

        @Override
        public EnergyAmount canReceiveValue(HubNode.HubMetadata hubMetadata) {
            return EnergyAmount.obtain(max);
        }

        @Override
        public boolean canExtract(IEnergyHandler iEnergyHandler, HubNode.HubMetadata hubMetadata) {
            if (!getCtrlStructureFormed()) {
                return false;
            }
            return DreamEnergyCore.getEnergyStored(getCtrl()).compareTo(BigInteger.ZERO) > 0;
        }

        @Override
        public boolean canReceive(IEnergyHandler iEnergyHandler, HubNode.HubMetadata hubMetadata) {
            return getCtrlStructureFormed();
        }

        @Override
        public void recycle() {

        }

        @Override
        public EnergyType getType(HubNode.HubMetadata hubMetadata) {
            if (!getCtrlStructureFormed()) {
                return EnergyType.INVALID;
            }
            return EnergyType.STORAGE;
        }
    }
}
