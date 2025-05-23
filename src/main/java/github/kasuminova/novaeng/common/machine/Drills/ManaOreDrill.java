package github.kasuminova.novaeng.common.machine.Drills;

import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.util.ResourceLocation;

public class ManaOreDrill extends Drill {

    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, "mana_ore_drill");
    public static final ManaOreDrill INSTANCE = new ManaOreDrill();

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }
}
