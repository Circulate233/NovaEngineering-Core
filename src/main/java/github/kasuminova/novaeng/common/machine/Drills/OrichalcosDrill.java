package github.kasuminova.novaeng.common.machine.Drills;

import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.util.ResourceLocation;

public class OrichalcosDrill extends Drill {

    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, "orichalcos_drill");
    public static final OrichalcosDrill ORICHALCOS_DRILL = new OrichalcosDrill();

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }
}
