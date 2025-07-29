package github.kasuminova.novaeng.common.hypernet.old.upgrade;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.upgrade.DynamicMachineUpgrade;
import github.kasuminova.mmce.common.upgrade.UpgradeType;
import github.kasuminova.novaeng.common.hypernet.old.upgrade.type.ProcessorModuleType;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

import java.util.List;

@ZenRegister
@ZenClass("novaeng.hypernet.upgrade.ProcessorModuleType")
public abstract class DataProcessorModule extends DynamicMachineUpgrade {
    private static final NBTTagCompound tag = new NBTTagCompound();

    public DataProcessorModule(final UpgradeType type) {
        super(type);
    }

    @ZenGetter("energyConsumption")
    public abstract long getEnergyConsumption();

    protected void getEnergyDurabilityTip(final List<String> desc, ProcessorModuleType moduleType) {
        desc.add(I18n.format("upgrade.data_processor.module.energy.tip",
                MiscUtils.formatNumber(getEnergyConsumption()) + " RF"));
    }

    @Override
    public void readItemNBT(final NBTTagCompound tag) {

    }

    @Override
    public NBTTagCompound writeItemNBT() {
        return tag;
    }

    public void writeNBTToItem() {
//        if (parentStack == null) {
//            return;
//        }
//
//        CapabilityUpgrade capability = parentStack.getCapability(CapabilityUpgrade.MACHINE_UPGRADE_CAPABILITY, null);
//        if (capability == null) {
//            return;
//        }
//
//        List<MachineUpgrade> upgrades = capability.getUpgrades();
//
//        for (final MachineUpgrade upgrade : upgrades) {
//            if (!upgradeEquals(upgrade)) {
//                continue;
//            }
//
//            DataProcessorModule processorModule = (DataProcessorModule) upgrade;
//            processorModule.readItemNBT(writeItemNBT());
//            if (parentBus != null) {
//                parentBus.markNoUpdateSync();
//            }
//            return;
//        }
    }

    public boolean upgradeEquals(final Object obj) {
        if (!(obj instanceof DataProcessorModule)) {
            return false;
        }
        return type.equals(((DataProcessorModule) obj).type);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj;
    }
}
