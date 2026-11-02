package github.kasuminova.novaeng.mixin.mekanism;

import mekanism.api.gas.GasStack;
import mekanism.common.MekanismFluids;
import mekanism.common.integration.OreDictManager;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.util.StackUtils;
import mekanism.common.world.DummyWorld;
import net.minecraft.block.Block;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import thelm.packagedauto.container.ContainerEmpty;

@Mixin(value = OreDictManager.class, remap = false)
public abstract class MixinSawmillLogRecipes {

    @Shadow
    private static void addSawmillLog(InventoryCrafting tempCrafting, ItemStack log, DummyWorld world) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    /**
     * @author circulation
     * @reason 清理多余逻辑
     */
    @Overwrite
    private static void addLogRecipes() {
        Container tempContainer = new ContainerEmpty();
        DummyWorld dummyWorld = new DummyWorld();
        InventoryCrafting tempCrafting = new InventoryCrafting(tempContainer, 3, 3);

        for (int i = 1; i < 9; ++i) {
            tempCrafting.setInventorySlotContents(i, ItemStack.EMPTY);
        }

        var list = OreDictionary.getOres("logWood", false);
        var size = list.size();
        for (var i = 0; i < size; i++) {
            var logEntry = list.get(i);
            if (logEntry.getItemDamage() == 32767) {
                NonNullList<ItemStack> logVariants = NonNullList.create();
                Block logBlock = Block.getBlockFromItem(logEntry.getItem());
                logBlock.getSubBlocks(logBlock.getCreativeTab(), logVariants);
                logVariants.forEach((stack) -> addSawmillLog(tempCrafting, StackUtils.size(stack, 1), dummyWorld));
            } else {
                addSawmillLog(tempCrafting, StackUtils.size(logEntry, 1), dummyWorld);
            }

            RecipeHandler.addPRCRecipe(StackUtils.size(logEntry, 1), new FluidStack(FluidRegistry.WATER, 100), new GasStack(MekanismFluids.Oxygen, 100), ItemStack.EMPTY, new GasStack(MekanismFluids.Hydrogen, 100), 0.0F, 150);
        }
    }
}
