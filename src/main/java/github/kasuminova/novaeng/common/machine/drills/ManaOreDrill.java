package github.kasuminova.novaeng.common.machine.drills;

import crafttweaker.api.item.IIngredient;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraftforge.fluids.FluidRegistry;

public final class ManaOreDrill extends Drill {

    public static final ManaOreDrill INSTANCE = new ManaOreDrill();
    private final IIngredient[] ingredients = {
        CraftTweakerMC.getILiquidStack(FluidRegistry.getFluidStack("fluidedmana", 1))
    };

    private ManaOreDrill() {
    }

    @Override
    public String getCoreTheardName() {
        return "novaeng.drill.thread.c";
    }

    @Override
    public String getMachineName() {
        return "mana_ore_drill";
    }

    @Override
    public Type getType() {
        return Type.SINGLE;
    }

    @Override
    public long getBaseEnergy() {
        return 24576;
    }

    @Override
    public boolean isDimensional() {
        return true;
    }

    @Override
    public IIngredient[] getExIngredient() {
        return this.ingredients;
    }
}
