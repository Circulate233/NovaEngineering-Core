package github.kasuminova.novaeng.common.hypernet.old.upgrade.type;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("novaeng.hypernet.upgrade.type.ProcessorModuleType")
public abstract class ProcessorModuleType {
    protected final int energyConsumption;

    public ProcessorModuleType(final int energyConsumption) {
        this.energyConsumption = energyConsumption;
    }

    @ZenMethod
    public abstract ProcessorModuleType register(String typeName, String localizedName, int level);

    @ZenGetter("energyConsumption")
    public int getEnergyConsumption() {
        return energyConsumption;
    }
}
