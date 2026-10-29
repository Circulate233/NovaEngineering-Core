package github.kasuminova.novaeng.common.item;

import appeng.api.networking.crafting.ICraftingGrid;
import appeng.util.item.AEItemStack;
import com.brandon3055.draconicevolution.api.itemconfig.BooleanConfigField;
import com.brandon3055.draconicevolution.api.itemconfig.IConfigurableItem;
import com.brandon3055.draconicevolution.api.itemconfig.IItemConfigField;
import com.brandon3055.draconicevolution.api.itemconfig.IntegerConfigField;
import com.brandon3055.draconicevolution.api.itemconfig.ItemConfigFieldRegistry;
import com.brandon3055.draconicevolution.api.itemconfig.ToolConfigHelper;
import com.circulation.random_complement.common.interfaces.RCCraftingGridCache;
import com.circulation.random_complement.common.util.MEHandler;
import github.kasuminova.mmce.common.util.DynamicPattern;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.CommonProxy;
import github.kasuminova.novaeng.common.util.AssemblyBlockArray;
import github.kasuminova.novaeng.common.util.AutoCraftingQueue;
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import ink.ikx.mmce.common.utils.StructureIngredient;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.List;

@SuppressWarnings("deprecation")
public final class ItemMachineAssemblyTool extends ItemBasic implements IConfigurableItem {
    public static final ItemMachineAssemblyTool INSTANCE = new ItemMachineAssemblyTool();

    private ItemMachineAssemblyTool() {
        super("machine_assembly_tool");
    }

    public static boolean isUsingAE(final ItemStack stack) {
        return ToolConfigHelper.getFieldStorage(stack).getBoolean("UsingAE");
    }

    public static boolean isIgnoreFluids(final ItemStack stack) {
        return ToolConfigHelper.getFieldStorage(stack).getBoolean("IgnoreFluids");
    }

    public static int getDynamicPatternSize(final ItemStack stack) {
        return ToolConfigHelper.getFieldStorage(stack).getInteger("DynamicPatternSize");
    }

    public static boolean isAutoAECrafting(final ItemStack stack) {
        return ToolConfigHelper.getFieldStorage(stack).getBoolean("AutoAECrafting");
    }

    public static boolean isNeedAllIngredient(final ItemStack stack) {
        return ToolConfigHelper.getFieldStorage(stack).getBoolean("NeedAllIngredient");
    }

    public static void setUsingAE(final ItemStack stack, final boolean value) {
        ToolConfigHelper.getFieldStorage(stack).setBoolean("UsingAE", value);
    }

    public static void setIgnoreFluids(final ItemStack stack, final boolean value) {
        ToolConfigHelper.getFieldStorage(stack).setBoolean("IgnoreFluids", value);
    }

    public static void setDynamicPatternSize(final ItemStack stack, final int value) {
        ToolConfigHelper.getFieldStorage(stack).setByte("DynamicPatternSize", (byte) value);
    }

    public static void setAutoAECrafting(final ItemStack stack, final boolean value) {
        ToolConfigHelper.getFieldStorage(stack).setBoolean("AutoAECrafting", value);
    }

    public static void setNeedAllIngredient(final ItemStack stack, final boolean value) {
        ToolConfigHelper.getFieldStorage(stack).setBoolean("NeedAllIngredient", value);
    }

    private static String description(final String key) {
        return I18n.translateToLocal("text.machine_assembly_tool.config." + key);
    }

    private static int getMaxDynamicPatternSize() {
        final var machines = new ObjectArrayList<>(MachineRegistry.getLoadedMachines());
        machines.addAll(NEWMachineAssemblyManager.getAllDynamicMachines());
        return machines.parallelStream().mapToInt(machine -> machine.getDynamicPatterns().values().stream()
                                                                    .mapToInt(DynamicPattern::getMaxSize).max().orElse(1)).max().orElse(1);
    }

    public BooleanConfigField getUsingAEConfig() {
        return Configs.USING_AE;
    }

    public BooleanConfigField getIgnoreFluidsConfig() {
        return Configs.IGNORE_FLUIDS;
    }

    public IntegerConfigField getDynamicPatternSizeConfig() {
        return Configs.DYNAMIC_SIZE;
    }

    public BooleanConfigField getAutoAECraftingConfig() {
        return Configs.AUTO_CRAFT;
    }

    public BooleanConfigField getNeedAllIngredientConfig() {
        return Configs.REQUIRE_ALL;
    }

    @Override
    public @NonNull ActionResult<ItemStack> onItemRightClick(final @NonNull World world, final @NonNull EntityPlayer player, final @NonNull EnumHand hand) {
        if (hand != EnumHand.MAIN_HAND || player.isSneaking()) {
            return super.onItemRightClick(world, player, hand);
        }
        if (world.isRemote) {
            player.openGui(NovaEngineeringCore.instance, CommonProxy.GuiType.MACHINE_ASSEMBLY_TOOL.ordinal(), world, 0, 0, 0);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public @NonNull EnumActionResult onItemUse(final @NonNull EntityPlayer player, final @NonNull World world, final @NonNull BlockPos pos, final @NonNull EnumHand hand,
                                               final @NonNull EnumFacing facing, final float hitX, final float hitY, final float hitZ) {
        if (hand != EnumHand.MAIN_HAND || world.isRemote) {
            return EnumActionResult.PASS;
        }
        final var tile = world.getTileEntity(pos);
        if (tile == null || !player.isSneaking()) {
            return EnumActionResult.PASS;
        }
        if (NEWMachineAssemblyManager.checkMachineAssembly(player)) {
            player.sendMessage(new TextComponentTranslation("message.assembly.tip.already_assembly"));
            return EnumActionResult.FAIL;
        }
        var state = world.getBlockState(pos);
        final var block = state.getBlock();
        state = block.getActualState(state, world, pos);
        DynamicMachine machine = null;
        EnumFacing rotation = null;
        if (tile instanceof TileMultiblockMachineController controller) {
            machine = controller.getBlueprintMachine();
            if (machine == null) {
                if (block instanceof BlockController controllerBlock) {
                    machine = controllerBlock.getParentMachine();
                } else {
                    return EnumActionResult.FAIL;
                }
            }
            rotation = player.world.getBlockState(pos).getValue(BlockController.FACING);
        } else {
            final int meta = block.getMetaFromState(state);
            final var iterator = NEWMachineAssemblyManager.getConstructorsIterator();
            while (iterator.hasNext()) {
                final var entry = iterator.next();
                if (entry.getKey() == block && entry.getValue().containsKey(meta)) {
                    machine = entry.getValue().get(meta);
                    final var property = block.getBlockState().getProperty("facing");
                    final Object value = property == null ? null : state.getValue(property);
                    rotation = value instanceof EnumFacing direction ? direction : facing;
                    break;
                }
            }
            if (rotation == EnumFacing.UP || rotation == EnumFacing.DOWN) {
                rotation = EnumFacing.NORTH;
            }
            if (machine == null || rotation == null) {
                return EnumActionResult.FAIL;
            }
        }
        final var stack = player.getHeldItem(hand);
        var array = new AssemblyBlockArray(BlockArrayCache.getBlockArrayCache(machine.getPattern(), rotation));
        int dynamicSize = getDynamicPatternSize(stack);
        for (final var pattern : machine.getDynamicPatterns().values()) {
            dynamicSize = Math.max(dynamicSize, pattern.getMinSize());
        }
        for (final var pattern : machine.getDynamicPatterns().values()) {
            pattern.addPatternToBlockArray(array, Math.clamp(dynamicSize, pattern.getMinSize(), pattern.getMaxSize()),
                pattern.getFaces().iterator().next(), rotation);
        }
        final var ingredients = StructureIngredient.of(player.world, pos, array.copy());
        array = array.offset(pos);
        if (!world.isOutsideBuildHeight(array.getMin()) || !world.isOutsideBuildHeight(array.getMax())) {
            player.sendMessage(new TextComponentTranslation("message.assembly.tip.too_high",
                "y = " + (array.getMin().getY() < 1 ? array.getMin().getY() : array.getMax().getY())));
            return EnumActionResult.FAIL;
        }
        final boolean usingAE = isUsingAE(stack);
        final boolean autoCraft = usingAE && isAutoAECrafting(stack);
        final var missing = NEWMachineAssemblyManager.checkAllItems(player, ingredients, usingAE, autoCraft);
        final var requests = missing.list();
        if (autoCraft && !requests.isEmpty()) {
            final var terminal = MEHandler.getTerminalGuiObject(player);
            if (terminal != null && terminal.getActionableNode() != null && terminal.getActionableNode().getGrid() != null) {
                final var grid = terminal.getActionableNode().getGrid();
                final RCCraftingGridCache crafting = grid.getCache(ICraftingGrid.class);
                final var craftable = crafting.rc$getCraftableItems();
                final var queue = new ArrayDeque<ItemStack>();
                for (final var alternatives : requests) {
                    for (final var item : alternatives) {
                        if (!item.isEmpty() && craftable.containsKey(AEItemStack.fromItemStack(item))) {
                            queue.add(item);
                            break;
                        }
                    }
                }
                AutoCraftingQueue.setQueueAndStrat(queue, player);
            }
        }
        if (requests.isEmpty() || !isNeedAllIngredient(stack)) {
            array = NEWMachineAssemblyManager.addAssemblyMachine(player, array);
            array.setMissing(missing.miss());
            array.start(usingAE, isIgnoreFluids(stack));
            player.sendMessage(new TextComponentTranslation("message.assembly.tip.already_assembly.start"));
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(final ItemStack stack, @Nullable final World world, final List<String> lines, final ITooltipFlag flag) {
        super.addInformation(stack, world, lines, flag);
        lines.add(I18n.translateToLocal("item.novaeng_core.machine_assembly_tool.config"));
        lines.add(" " + I18n.translateToLocal(Configs.USING_AE.getUnlocalizedName()) + " : " + isUsingAE(stack));
        lines.add(" " + I18n.translateToLocal(Configs.IGNORE_FLUIDS.getUnlocalizedName()) + " : " + isIgnoreFluids(stack));
        lines.add(" " + I18n.translateToLocal(Configs.DYNAMIC_SIZE.getUnlocalizedName()) + " : " + getDynamicPatternSize(stack));
        lines.add(" " + I18n.translateToLocal(Configs.AUTO_CRAFT.getUnlocalizedName()) + " : " + isAutoAECrafting(stack));
        lines.add(" " + I18n.translateToLocal(Configs.REQUIRE_ALL.getUnlocalizedName()) + " : " + isNeedAllIngredient(stack));
    }

    @Override
    public ItemConfigFieldRegistry getFields(final ItemStack stack, final ItemConfigFieldRegistry registry) {
        registry.register(stack, Configs.USING_AE);
        registry.register(stack, Configs.IGNORE_FLUIDS);
        registry.register(stack, Configs.DYNAMIC_SIZE);
        registry.register(stack, Configs.AUTO_CRAFT);
        registry.register(stack, Configs.REQUIRE_ALL);
        return registry;
    }

    @Override
    public int getProfileCount(final ItemStack stack) {
        return 5;
    }

    private static final class Configs {
        private static final BooleanConfigField USING_AE = new BooleanConfigField("UsingAE", false, description("UsingAE"));
        private static final BooleanConfigField IGNORE_FLUIDS = new BooleanConfigField("IgnoreFluids", false, description("IgnoreFluids"));
        private static final IntegerConfigField DYNAMIC_SIZE = new IntegerConfigField("DynamicPatternSize", 0, 0,
            getMaxDynamicPatternSize(), description("DynamicPatternSize"), IItemConfigField.EnumControlType.SELECTIONS);
        private static final BooleanConfigField AUTO_CRAFT = new BooleanConfigField("AutoAECrafting", false, description("AutoAECrafting"));
        private static final BooleanConfigField REQUIRE_ALL = new BooleanConfigField("NeedAllIngredient", false, description("NeedAllIngredient"));
    }
}
