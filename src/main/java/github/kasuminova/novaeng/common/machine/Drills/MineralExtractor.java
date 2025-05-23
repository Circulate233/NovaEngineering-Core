package github.kasuminova.novaeng.common.machine.Drills;

import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.util.ResourceLocation;

public class MineralExtractor extends Drill{

    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, "mineral_extractor");
    public static final MineralExtractor INSTANCE = new MineralExtractor();

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }
}
