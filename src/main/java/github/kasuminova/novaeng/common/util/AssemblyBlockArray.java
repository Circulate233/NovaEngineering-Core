package github.kasuminova.novaeng.common.util;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.me.helpers.PlayerSource;
import com.circulation.random_complement.common.util.MEHandler;
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager.Ingredient;
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager.OperatingStatus;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import ink.ikx.mmce.common.assembly.MachineAssembly;
import ink.ikx.mmce.common.utils.FluidUtils;
import ink.ikx.mmce.common.utils.StackUtils;
import ink.ikx.mmce.common.utils.StructureIngredient;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

public final class AssemblyBlockArray extends BlockArray {
    private static final Object2ReferenceOpenHashMap<BlockInformation, ObjectArrayList<Tuple<Ingredient, IBlockState>>> MATERIAL = new Object2ReferenceOpenHashMap<>();
    private static final Object2ReferenceOpenHashMap<BlockInformation, PlacementStage> STAGES = new Object2ReferenceOpenHashMap<>();
    private boolean usingAE;
    private boolean ignoreFluids;
    private int missing;
    private Queue<QueuedPlacement> queue;

    public AssemblyBlockArray() {
    }

    public AssemblyBlockArray(final long uid) {
        super(uid);
    }

    public AssemblyBlockArray(final BlockArray other) {
        super(other);
    }

    public AssemblyBlockArray(final BlockArray other, final BlockPos offset) {
        super(other, offset);
    }

    private static List<FluidInventory> getFluidHandlerItems(final List<ItemStack> inventory) {
        final List<FluidInventory> result = new ObjectArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            final var stack = inventory.get(i);
            if (!FluidUtils.isFluidHandler(stack)) {
                continue;
            }
            final var handler = FluidUtil.getFluidHandler(stack);
            if (handler != null) {
                result.add(new FluidInventory(i, handler));
            }
        }
        return result;
    }

    private static boolean consumeInventoryFluid(final FluidStack required, final List<FluidInventory> handlers,
                                                 final InventoryPlayer player) {
        for (final var entry : handlers) {
            final var drained = entry.fluid().drain(required.copy(), false);
            if (drained != null && drained.containsFluid(required)) {
                entry.fluid().drain(required.copy(), true);
                if (player != null) {
                    player.setInventorySlotContents(entry.slot(), entry.fluid().getContainer());
                }
                return true;
            }
        }
        return false;
    }

    public static void searchAndRemoveContainFluid(final List<ItemStack> inventory,
                                                   final List<StructureIngredient.FluidIngredient> ingredients) {
        final var handlers = getFluidHandlerItems(inventory);
        final var iterator = ingredients.iterator();
        while (iterator.hasNext()) {
            for (final var pair : iterator.next().ingredientList()) {
                if (consumeInventoryFluid(pair.getFirst(), handlers, null)) {
                    iterator.remove();
                    break;
                }
            }
        }
    }

    private static ObjectArrayList<Tuple<Ingredient, IBlockState>> buildMaterialList(final BlockInformation info) {
        final var resolved = new ObjectArrayList<Tuple<Ingredient, IBlockState>>();
        boolean fluidOnly = true;
        for (final var descriptor : info.getMatchingStates()) {
            for (final var state : descriptor.getApplicable()) {
                final var block = state.getBlock();
                final Ingredient ingredient;
                if (block instanceof BlockFluidBase fluid) {
                    ingredient = new Ingredient(new FluidStack(fluid.getFluid(), 1000));
                } else if (block instanceof BlockLiquid) {
                    ingredient = new Ingredient(new FluidStack(state.getMaterial() == Material.LAVA ? FluidRegistry.LAVA : FluidRegistry.WATER, 1000));
                } else {
                    fluidOnly = false;
                    ingredient = new Ingredient(StackUtils.getStackFromBlockState(state));
                }
                if (ingredient.isItem()) {
                    fluidOnly = false;
                }
                resolved.add(new Tuple<>(ingredient, state));
            }
        }
        MATERIAL.put(info, resolved);
        STAGES.put(info, !resolved.isEmpty() && fluidOnly ? PlacementStage.FLUID : PlacementStage.SOLID);
        return resolved;
    }

    public static ObjectArrayList<Tuple<Ingredient, IBlockState>> getMaterialList(final BlockInformation info) {
        final var existing = MATERIAL.get(info);
        return existing == null ? buildMaterialList(info) : existing;
    }

    public static PlacementStage getPlacementStage(final BlockInformation info) {
        PlacementStage stage = STAGES.get(info);
        if (stage == null) {
            buildMaterialList(info);
            stage = STAGES.getOrDefault(info, PlacementStage.SOLID);
        }
        return stage;
    }

    private static boolean isFluid(final IBlockState state) {
        return state.getBlock() instanceof BlockLiquid || state.getBlock() instanceof BlockFluidBase;
    }

    public boolean getUsingAE() {
        return this.usingAE;
    }

    public void setUsingAE(final boolean value) {
        this.usingAE = value;
    }

    public boolean getIgnoreFluids() {
        return this.ignoreFluids;
    }

    public void setIgnoreFluids(final boolean value) {
        this.ignoreFluids = value;
    }

    public int getMissing() {
        return this.missing;
    }

    public void setMissing(final int value) {
        this.missing = value;
    }

    public AssemblyBlockArray copy() {
        return new AssemblyBlockArray(this);
    }

    public AssemblyBlockArray offset(final BlockPos offset) {
        return new AssemblyBlockArray(this, offset);
    }

    public void end() {
        this.pattern.clear();
        this.queue = null;
    }

    public void start() {
        this.start(true, true);
    }

    public void start(final boolean usingAE) {
        this.start(usingAE, true);
    }

    public void start(final boolean usingAE, final boolean ignoreFluids) {
        this.usingAE = usingAE;
        this.ignoreFluids = ignoreFluids;
        final var solid = new ArrayDeque<QueuedPlacement>();
        final var fluid = new ArrayDeque<QueuedPlacement>();
        for (final var entry : this.pattern.entrySet()) {
            getMaterialList(entry.getValue());
            final var stage = getPlacementStage(entry.getValue());
            if (ignoreFluids && stage == PlacementStage.FLUID) {
                continue;
            }
            final var placement = new QueuedPlacement(entry.getKey(), entry.getValue(), stage);
            (stage == PlacementStage.FLUID ? fluid : solid).add(placement);
        }
        this.queue = new ArrayDeque<>(solid.size() + fluid.size());
        this.queue.addAll(solid);
        this.queue.addAll(fluid);
    }

    private QueuedPlacement pollPlacement() {
        while (this.queue != null) {
            final var next = this.queue.poll();
            if (next == null) {
                return null;
            }
            synchronized (this.pattern) {
                if (this.pattern.remove(next.pos()) != null) {
                    return next;
                }
            }
        }
        return null;
    }

    public OperatingStatus assemblyBlock(final World world, final EntityPlayerMP player) {
        final var placement = this.pollPlacement();
        if (placement == null) {
            return OperatingStatus.COMPLETE;
        }
        final var pos = placement.pos();
        final var info = placement.info();
        if (player.isCreative()) {
            this.placeBlock(player, world, pos, info.getSampleState());
            return OperatingStatus.SUCCESS;
        }
        final var old = world.getBlockState(pos);
        if (old != null && old.getBlock() != Blocks.AIR && !(this.ignoreFluids && isFluid(old))) {
            if (this.matchesState(info, old)) {
                return OperatingStatus.ALREADY_EXISTS;
            }
            player.sendMessage(new TextComponentTranslation("message.assembly.tip.cannot_replace", MiscUtils.posToString(pos)));
            return OperatingStatus.FAILURE;
        }
        final var materials = getMaterialList(info);
        WirelessTerminalGuiObject terminal = null;
        IMEMonitor<IAEItemStack> items = null;
        IMEMonitor<IAEFluidStack> fluids = null;
        if (this.usingAE) {
            terminal = MEHandler.getTerminalGuiObject(player);
            if (terminal != null && terminal.getActionableNode() != null && terminal.getActionableNode().getGrid() != null) {
                final IStorageGrid storage = terminal.getActionableNode().getGrid().getCache(IStorageGrid.class);
                items = storage.getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
                fluids = storage.getInventory(AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class));
            }
        }
        final var inventory = player.inventory.mainInventory;
        final var fluidInventory = getFluidHandlerItems(inventory);
        for (final var pair : materials) {
            final var ingredient = pair.getFirst();
            if (ingredient.isItem()) {
                if (ingredient.getItemStack().isEmpty()) {
                    continue;
                }
                if (MachineAssembly.consumeInventoryItem(ingredient.getItemStack(), inventory)) {
                    this.placeBlock(player, world, pos, pair.getSecond());
                    return OperatingStatus.SUCCESS;
                }
            } else if (consumeInventoryFluid(ingredient.getFluidStack(), fluidInventory, player.inventory)) {
                this.placeBlock(player, world, pos, pair.getSecond());
                return OperatingStatus.SUCCESS;
            }
        }
        if (items != null) {
            for (final var pair : materials) {
                final var ingredient = pair.getFirst();
                if (ingredient.isItem()) {
                    if (ingredient.getItemStack().isEmpty()) {
                        continue;
                    }
                    final var extracted = items.extractItems(ingredient.getAEItemStack(), Actionable.MODULATE, new PlayerSource(player, terminal));
                    if (extracted == null || extracted.getStackSize() == 0) {
                        continue;
                    }
                } else {
                    final var extracted = fluids.extractItems(ingredient.getAEFluidStack(), Actionable.SIMULATE, new PlayerSource(player, terminal));
                    if (extracted == null || extracted.getStackSize() < 1000) {
                        continue;
                    }
                    fluids.extractItems(ingredient.getAEFluidStack(), Actionable.MODULATE, new PlayerSource(player, terminal));
                }
                this.placeBlock(player, world, pos, pair.getSecond());
                return OperatingStatus.SUCCESS;
            }
        }
        if (old.getBlock() == Blocks.AIR && this.matchesState(info, old)) {
            return OperatingStatus.ALREADY_EXISTS;
        }
        if (this.missing > 0) {
            this.missing--;
            return OperatingStatus.SUCCESS;
        }
        player.sendMessage(new TextComponentTranslation("message.assembly.tip.missing", MiscUtils.posToString(pos)));
        return OperatingStatus.FAILURE;
    }

    private void placeBlock(final EntityPlayerMP player, final World world, final BlockPos pos, final IBlockState state) {
        Objects.requireNonNull(player.getServer()).addScheduledTask(() -> {
            if (!player.isCreative() && ForgeEventFactory.onBlockPlace(player, new BlockSnapshot(world, pos, state), EnumFacing.UP).isCanceled()) {
                player.sendMessage(new TextComponentTranslation("message.assembly.tip.missing", MiscUtils.posToString(pos)));
                player.inventory.placeItemBackInInventory(world, StackUtils.getStackFromBlockState(state));
                return;
            }
            final int flags = 0b10011;
            final var chunk = world.getChunk(pos);
            final var snapshot = world.captureBlockSnapshots ? BlockSnapshot.getBlockSnapshot(world, pos, flags) : null;
            final var old = chunk.setBlockState(pos, state);
            if (old != null) {
                if (snapshot == null) {
                    world.markAndNotifyBlock(pos, chunk, old, state, flags);
                } else {
                    world.capturedBlockSnapshots.add(snapshot);
                }
            }
        });
    }

    public boolean matchesState(final BlockInformation info, final IBlockState state) {
        final var block = state.getBlock();
        final int meta = block.getMetaFromState(state);
        for (final var descriptor : info.getMatchingStates()) {
            for (final var applicable : descriptor.getApplicable()) {
                final var type = applicable.getBlock();
                if (type == block && type.getMetaFromState(applicable) == meta) {
                    return true;
                }
            }
        }
        return false;
    }

    public enum PlacementStage {SOLID, FLUID}

    public record QueuedPlacement(BlockPos pos, BlockInformation info, PlacementStage stage) {
    }

    private record FluidInventory(int slot, IFluidHandlerItem fluid) {
    }
}
