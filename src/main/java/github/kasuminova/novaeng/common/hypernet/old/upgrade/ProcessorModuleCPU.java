package github.kasuminova.novaeng.common.hypernet.old.upgrade;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import github.kasuminova.mmce.common.upgrade.UpgradeType;
import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils;
import github.kasuminova.novaeng.common.hypernet.old.upgrade.type.ProcessorModuleCPUType;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ZenRegister
@ZenClass("novaeng.hypernet.upgrade.ProcessorModuleCPU")
public class ProcessorModuleCPU extends DataProcessorModule {

    protected final ProcessorModuleCPUType moduleType = RegistryHyperNet.getDataProcessorModuleCPUType(getType());

    public ProcessorModuleCPU(final UpgradeType type) {
        super(type);
    }

    public static List<ProcessorModuleCPU> filter(final Collection<List<MachineUpgrade>> upgradeLists) {
        List<ProcessorModuleCPU> list = new ArrayList<>();
        for (List<MachineUpgrade> upgradeList : upgradeLists) {
            for (final MachineUpgrade upgrade : upgradeList) {
                if (upgrade instanceof final ProcessorModuleCPU cpu) {
                    list.add(cpu);
                }
            }
        }
        return list;
    }

    public double calculate(final boolean doCalculate, double maxGeneration) {
        double generationBase = getComputationPointGeneration();
        double left = Math.min((generationBase - maxGeneration), generationBase);

        if (left <= 0) {
            return generationBase;
        } else {
            return generationBase - left;
        }
    }

    public float getEfficiency() {
        return 1.0F;
    }

    @Override
    public long getEnergyConsumption() {
        return moduleType.getEnergyConsumption();
    }

    @ZenGetter("computationalPointGeneration")
    public double getComputationPointGeneration() {
        return moduleType.getComputationPointGeneration();
    }

    @Override
    public List<String> getDescriptions() {
        List<String> desc = new ArrayList<>();
        desc.add(I18n.format("upgrade.data_processor.module.cpu.tip.0"));

        desc.add(I18n.format("upgrade.data_processor.module.cpu.generate",
            NovaEngUtils.formatFLOPS(calculate(false, getComputationPointGeneration()))
        ));

        getEnergyDurabilityTip(desc, moduleType);

        return desc;
    }

    @Override
    public List<String> getBusGUIDescriptions() {
        List<String> desc = new ArrayList<>();

        desc.add(I18n.format("upgrade.data_processor.module.cpu.generate",
            NovaEngUtils.formatFLOPS(calculate(false, getComputationPointGeneration()))
        ));

        getEnergyDurabilityTip(desc, moduleType);

        return desc;
    }

    @Override
    public ProcessorModuleCPU copy(ItemStack parentStack) {
        ProcessorModuleCPU upgrade = new ProcessorModuleCPU(getType());
        upgrade.eventProcessor.putAll(eventProcessor);
        upgrade.parentStack = parentStack;
        return upgrade;
    }

    public boolean upgradeEquals(final Object obj) {
        if (!(obj instanceof ProcessorModuleCPU)) {
            return false;
        }
        return moduleType.equals(((ProcessorModuleCPU) obj).moduleType);
    }
}
