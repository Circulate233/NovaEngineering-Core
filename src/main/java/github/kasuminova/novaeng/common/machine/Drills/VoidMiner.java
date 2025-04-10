package github.kasuminova.novaeng.common.machine.Drills;

import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.util.ResourceLocation;

public class VoidMiner extends Drill{
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, "void_miner");
    public static final VoidMiner VOID_MINER = new VoidMiner();

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }
}
