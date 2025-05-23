package github.kasuminova.novaeng.common.machine.Drills;

import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.util.ResourceLocation;

public class DifferentWorld extends Drill {

    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, "different_world");
    public static final DifferentWorld INSTANCE = new DifferentWorld();

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }
}
