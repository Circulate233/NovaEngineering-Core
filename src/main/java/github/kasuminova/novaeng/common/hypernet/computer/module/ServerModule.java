package github.kasuminova.novaeng.common.hypernet.computer.module;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.novaeng.common.hypernet.computer.ModularServer;
import github.kasuminova.novaeng.common.hypernet.computer.module.base.ServerModuleBase;
import lombok.Getter;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;

import javax.annotation.Nonnull;

@ZenRegister
@ZenClass("novaeng.hypernet.server.module.ServerModule")
public abstract class ServerModule {

    @Getter
    protected final ModularServer server;
    @Getter
    protected final ServerModuleBase<?> moduleBase;

    protected boolean broken;

    public ServerModule(final ModularServer server, final ServerModuleBase<?> moduleBase) {
        this.server = server;
        this.moduleBase = moduleBase;
    }

    public void readNBT(@Nonnull NBTTagCompound nbt) {
        broken = nbt.getBoolean("broken");
    }

    public void writeNBT(@Nonnull NBTTagCompound nbt) {
        if (broken) {
            nbt.setBoolean("broken", true);
        }
    }

}
