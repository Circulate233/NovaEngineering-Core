package github.kasuminova.novaeng.common.hypernet.old.upgrade.type;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.upgrade.UpgradeType;
import github.kasuminova.mmce.common.upgrade.registry.RegistryUpgrade;
import github.kasuminova.novaeng.common.hypernet.old.upgrade.ProcessorModuleRAM;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("novaeng.hypernet.upgrade.type.ProcessorModuleRAMType")
public class ProcessorModuleRAMType extends ProcessorModuleType {
    private final double computationPointGenerationLimit;

    public ProcessorModuleRAMType(final long energyConsumption,
                                  final double computationPointGenerationLimit)
    {
        super(energyConsumption);
        this.computationPointGenerationLimit = computationPointGenerationLimit;
    }

    /**
     * 已经删除耐久相关设定
     */
    @Deprecated
    @ZenMethod
    public static ProcessorModuleRAMType create(final int minDurability,
                                                final int maxDurability,
                                                final long energyConsumption,
                                                final double computationPointGenerationLimit)
    {
        return new ProcessorModuleRAMType(energyConsumption, computationPointGenerationLimit);
    }

    @ZenMethod
    public static ProcessorModuleRAMType create(final long energyConsumption,
                                                final double computationPointGenerationLimit)
    {
        return new ProcessorModuleRAMType(energyConsumption, computationPointGenerationLimit);
    }

    @Override
    public ProcessorModuleRAMType register(String typeName, String localizedName, int level) {
        UpgradeType type = new UpgradeType(typeName, localizedName, level, 1);
        RegistryHyperNet.addDataProcessorModuleRAMType(type, this);
        RegistryUpgrade.registerUpgrade(typeName, new ProcessorModuleRAM(type));

        return this;
    }

    @ZenGetter("computationPointGenerationLimit")
    public double getComputationPointGenerationLimit() {
        return computationPointGenerationLimit;
    }
}
