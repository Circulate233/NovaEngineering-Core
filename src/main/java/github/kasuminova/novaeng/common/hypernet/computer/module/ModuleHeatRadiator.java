package github.kasuminova.novaeng.common.hypernet.computer.module;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.novaeng.common.hypernet.computer.ModularServer;
import github.kasuminova.novaeng.common.hypernet.computer.module.base.ServerModuleBase;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("novaeng.hypernet.server.module.ModuleHeatRadiator")
public abstract class ModuleHeatRadiator extends ServerModule {

    protected int moduleAmount;

    public ModuleHeatRadiator(final ModularServer server, final ServerModuleBase<?> moduleBase, final int moduleAmount) {
        super(server, moduleBase);
        this.moduleAmount = moduleAmount;
    }

    public int getModuleAmount() {
        return moduleAmount;
    }

    public ModuleHeatRadiator setModuleAmount(final int moduleAmount) {
        this.moduleAmount = moduleAmount;
        return this;
    }
}
