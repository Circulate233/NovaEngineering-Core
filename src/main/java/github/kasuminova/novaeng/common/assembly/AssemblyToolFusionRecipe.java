package github.kasuminova.novaeng.common.assembly;

import github.kasuminova.novaeng.common.item.ItemMachineAssemblyTool;
import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class AssemblyToolFusionRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    public AssemblyToolFusionRecipe() {
        setRegistryName(new ResourceLocation(Tags.MOD_ID, "assembly_tool_fusion"));
    }

    @SubscribeEvent
    public static void register(final RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new AssemblyToolFusionRecipe());
    }

    @Nullable
    private static Inputs inputs(final InventoryCrafting inventory) {
        ItemStack builder = ItemStack.EMPTY, tool = ItemStack.EMPTY;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            final var stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ItemMachineAssemblyTool && builder.isEmpty()) builder = stack;
            else if (AssemblyMiningTool.accepts(stack) && tool.isEmpty()) tool = stack;
            else return null;
        }
        return builder.isEmpty() || tool.isEmpty() ? null : new Inputs(builder, tool);
    }

    @Override
    public boolean matches(final InventoryCrafting inventory, final World world) {
        return inputs(inventory) != null;
    }

    @Override
    public ItemStack getCraftingResult(final InventoryCrafting inventory) {
        final var inputs = inputs(inventory);
        return inputs == null ? ItemStack.EMPTY : AssemblyMiningTool.fused(inputs.builder, inputs.tool);
    }

    @Override
    public boolean canFit(final int width, final int height) {
        return (long) width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final InventoryCrafting inventory) {
        return NonNullList.withSize(inventory.getSizeInventory(), ItemStack.EMPTY);
    }

    private record Inputs(ItemStack builder, ItemStack tool) {
    }
}
