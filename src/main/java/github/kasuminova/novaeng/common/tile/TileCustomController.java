package github.kasuminova.novaeng.common.tile;

import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.machine.RecipeThread;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public abstract class TileCustomController extends TileMultiblockMachineController {
    protected CraftingStatus controllerStatus = CraftingStatus.IDLE;

    @Override
    protected void readMachineNBT(NBTTagCompound compound) {
        if (compound.hasKey("parentMachine")) {
            ResourceLocation rl = new ResourceLocation(compound.getString("parentMachine"));
            parentMachine = MachineRegistry.getRegistry().getMachine(rl);
        }

        super.readMachineNBT(compound);
    }

    @Override
    public CraftingStatus getControllerStatus() {
        return controllerStatus;
    }

    @Override
    public void setControllerStatus(final CraftingStatus status) {
        this.controllerStatus = status;
    }

    @Override
    public void flushContextModifier() {

    }

    @Nullable
    @Override
    public ActiveMachineRecipe getActiveRecipe() {
        return null;
    }

    @Override
    public ActiveMachineRecipe[] getActiveRecipeList() {
        return new ActiveMachineRecipe[0];
    }

    @Override
    public RecipeThread[] getRecipeThreadList() {
        return new RecipeThread[0];
    }

    @Override
    public void addModifier(final String key, final RecipeModifier modifier) {

    }

    @Override
    public void removeModifier(final String key) {

    }

    @Override
    public void overrideStatusInfo(final String newInfo) {

    }
}
