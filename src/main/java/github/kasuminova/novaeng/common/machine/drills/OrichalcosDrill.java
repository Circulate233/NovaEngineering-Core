package github.kasuminova.novaeng.common.machine.drills;

import crafttweaker.api.item.IIngredient;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraftforge.fluids.FluidRegistry;

public final class OrichalcosDrill extends Drill {

    public static final OrichalcosDrill INSTANCE = new OrichalcosDrill();
    private final IIngredient[] ingredients = {
        CraftTweakerMC.getILiquidStack(FluidRegistry.getFluidStack("fluidedmana", 1))
    };

    private OrichalcosDrill() {
    }

    @Override
    public String getCoreTheardName() {
        return "novaeng.drill.thread.c";
    }

    @Override
    public String getMachineName() {
        return "orichalcos_drill";
    }

    @Override
    public Type getType() {
        return Type.RANGE;
    }

    @Override
    public float getAdvancedRecipeTimeMultiple() {
        return 1.5f;
    }

    @Override
    public long getBaseEnergy() {
        return 32768;
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
