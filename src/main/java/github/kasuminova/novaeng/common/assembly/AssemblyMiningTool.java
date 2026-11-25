package github.kasuminova.novaeng.common.assembly;

import appeng.api.implementations.items.IAEWrench;
import github.kasuminova.novaeng.common.item.ItemMachineAssemblyTool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class AssemblyMiningTool {
    private static final String KEY = "AssemblyMiningTool";

    private AssemblyMiningTool() {
    }

    public static boolean accepts(final ItemStack stack) {
        return !stack.isEmpty() && !(stack.getItem() instanceof ItemMachineAssemblyTool)
            && (!stack.getItem().getToolClasses(stack).isEmpty() || stack.getItem() instanceof IAEWrench
            || stack.getItem().isDamageable() && !(stack.getItem() instanceof ItemArmor));
    }

    public static ItemStack fused(final ItemStack builder, final ItemStack tool) {
        if (!(builder.getItem() instanceof ItemMachineAssemblyTool) || !accepts(tool)) return ItemStack.EMPTY;
        final var result = builder.copy();
        result.setCount(1);
        final var profile = tool.copy();
        profile.setCount(1);
        if (!result.hasTagCompound()) result.setTagCompound(new NBTTagCompound());
        result.getTagCompound().setTag(KEY, profile.serializeNBT());
        return result;
    }

    public static ItemStack profile(final ItemStack builder) {
        final var tag = builder.getTagCompound();
        if (tag == null || !tag.hasKey(KEY, 10)) return ItemStack.EMPTY;
        final var tool = new ItemStack(tag.getCompoundTag(KEY));
        return accepts(tool) ? tool : ItemStack.EMPTY;
    }

    static boolean canHarvest(final ItemStack tool, final IBlockState state, final EntityPlayer player) {
        if (state.getMaterial().isToolNotRequired()) return true;
        if (tool.isEmpty()) return state.getBlock().getHarvestLevel(state) <= 3;
        final var toolClass = state.getBlock().getHarvestTool(state);
        if (toolClass != null) {
            final int level = tool.getItem().getHarvestLevel(tool, toolClass, player, state);
            if (level >= 0) return level >= state.getBlock().getHarvestLevel(state);
        }
        return tool.getItem().canHarvestBlock(state, tool);
    }
}
