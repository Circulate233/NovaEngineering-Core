package github.kasuminova.novaeng.common.hypernet.proc.server.module;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.novaeng.common.hypernet.proc.server.CalculateServer;
import github.kasuminova.novaeng.common.hypernet.proc.server.ModularServer;
import github.kasuminova.novaeng.common.hypernet.proc.server.module.base.ServerModuleBase;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import javax.annotation.Nonnull;

@ZenRegister
@ZenClass("novaeng.hypernet.module.ModulePSU")
public class ModulePSU extends ServerModule {

    protected long maxEnergyProvision;

    public ModulePSU(final ModularServer server,final ServerModuleBase<?> moduleBase, final long maxEnergyProvision) {
        super(server, moduleBase);
        this.maxEnergyProvision = maxEnergyProvision;
    }

    @ZenMethod
    public static ModulePSU cast(ServerModule module) {
        return module instanceof ModulePSU ? (ModulePSU) module : null;
    }

    public long getMaxEnergyProvision() {
        return maxEnergyProvision;
    }

    public void setMaxEnergyProvision(final long maxEnergyProvision) {
        this.maxEnergyProvision = maxEnergyProvision;
    }
}
