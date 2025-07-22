package github.kasuminova.novaeng.common.hypernet.old.upgrade;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import github.kasuminova.mmce.common.upgrade.UpgradeType;
import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils;
import github.kasuminova.novaeng.common.hypernet.old.upgrade.type.ProcessorModuleRAMType;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ZenRegister
@ZenClass("novaeng.hypernet.upgrade.ProcessorModuleRAM")
public class ProcessorModuleRAM extends DataProcessorModule {
    private final ProcessorModuleRAMType moduleType = RegistryHyperNet.getDataProcessorModuleRAMType(getType());

    public ProcessorModuleRAM(final UpgradeType type) {
        super(type);
    }

    public static List<ProcessorModuleRAM> filter(final Collection<List<MachineUpgrade>> upgradeLists) {
        List<ProcessorModuleRAM> list = new ArrayList<>();
        for (List<MachineUpgrade> upgradeList : upgradeLists) {
            for (final MachineUpgrade upgrade : upgradeList) {
                if (upgrade instanceof final ProcessorModuleRAM ram) {
                    list.add(ram);
                }
            }
        }
        return list;
    }

    public double calculate(final boolean doCalculate, double maxGeneration) {
        double generationBase = getComputationPointGenerationLimit();
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
    public int getEnergyConsumption() {
        return moduleType.getEnergyConsumption();
    }

    @ZenGetter("computationPointGenerationLimit")
    public double getComputationPointGenerationLimit() {
        return moduleType.getComputationPointGenerationLimit();
    }

    @Override
    public ProcessorModuleRAM copy(ItemStack parentStack) {
        ProcessorModuleRAM upgrade = new ProcessorModuleRAM(getType());
        upgrade.eventProcessor.putAll(eventProcessor);
        upgrade.parentStack = parentStack;
        return upgrade;
    }

    @Override
    public List<String> getDescriptions() {
        List<String> desc = new ArrayList<>();
        desc.add(I18n.format("upgrade.data_processor.module.ram.tip.0"));

        desc.add(I18n.format("upgrade.data_processor.module.ram.limit_provision",
                NovaEngUtils.formatFLOPS(calculate(false, getComputationPointGenerationLimit()))
        ));

        getEnergyDurabilityTip(desc, moduleType);

        return desc;
    }

    @Override
    public List<String> getBusGUIDescriptions() {
        List<String> desc = new ArrayList<>();

        desc.add(I18n.format("upgrade.data_processor.module.ram.limit_provision",
                NovaEngUtils.formatFLOPS(calculate(false, getComputationPointGenerationLimit()))
        ));
        getEnergyDurabilityTip(desc, moduleType);

        return desc;
    }

    public boolean upgradeEquals(final Object obj) {
        if (!(obj instanceof ProcessorModuleRAM)) {
            return false;
        }
        return moduleType.equals(((ProcessorModuleRAM) obj).moduleType);
    }
}
