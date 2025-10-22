package github.kasuminova.novaeng.common.hypernet.old.upgrade.type;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.upgrade.UpgradeType;
import github.kasuminova.mmce.common.upgrade.registry.RegistryUpgrade;
import github.kasuminova.novaeng.common.hypernet.old.upgrade.ProcessorModuleCPU;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("novaeng.hypernet.upgrade.type.ProcessorModuleCPUType")
public class ProcessorModuleCPUType extends ProcessorModuleType {
    protected final double computationPointGeneration;

    public ProcessorModuleCPUType(final long energyConsumption,
                                  final double computationPointGeneration) {
        super(energyConsumption);
        this.computationPointGeneration = computationPointGeneration;
    }

    /**
     * 已经删除耐久相关设定
     */
    @Deprecated
    @ZenMethod
    public static ProcessorModuleCPUType create(final int minDurability,
                                                final int maxDurability,
                                                final long energyConsumption,
                                                final double computationPointGeneration) {
        return new ProcessorModuleCPUType(energyConsumption, computationPointGeneration);
    }

    @ZenMethod
    public static ProcessorModuleCPUType create(final long energyConsumption,
                                                final double computationPointGeneration) {
        return new ProcessorModuleCPUType(energyConsumption, computationPointGeneration);
    }

    @Override
    public ProcessorModuleCPUType register(String typeName, String localizedName, int level) {
        UpgradeType type = new UpgradeType(typeName, localizedName, level, 1);
        RegistryHyperNet.addDataProcessorModuleCPUType(type, this);
        RegistryUpgrade.registerUpgrade(typeName, new ProcessorModuleCPU(type));

        return this;
    }

    @ZenGetter("computationPointGeneration")
    public double getComputationPointGeneration() {
        return computationPointGeneration;
    }
}
