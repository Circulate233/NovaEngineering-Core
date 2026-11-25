package github.kasuminova.novaeng.common.assembly;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload;
import github.kasuminova.novaeng.common.assembly.effect.ServerConstructionEffects;
import github.kasuminova.novaeng.common.container.ContainerNEWCraftConfirm;
import github.kasuminova.novaeng.common.item.ItemMachineAssemblyTool;
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Objects;

public final class AssemblyConstructionTasks {
    private static final int GROUPS = ConstructionEffectPayload.MAX_LOGICAL_CHUNKS;
    private static final int SOLID_SLOTS = GROUPS * 2;
    private static final int STAGES = SOLID_SLOTS + GROUPS;
    private static final Reference2ObjectOpenHashMap<EntityPlayerMP, Task> ACTIVE = new Reference2ObjectOpenHashMap<>();
    private static final ArrayDeque<Task> QUEUE = new ArrayDeque<>();
    private static final AssemblyPlacement PLACEMENT = new AssemblyPlacement();

    private AssemblyConstructionTasks() {
    }

    public static boolean isBuilding(final EntityPlayerMP player) {
        return ACTIVE.containsKey(player);
    }

    public static boolean cancelAt(final EntityPlayerMP player, final BlockPos anchor) {
        final var task = ACTIVE.get(player);
        if (task == null || !task.anchor.equals(anchor)) return false;
        cancel(player);
        task.message("cancelled");
        return true;
    }

    public static void enqueue(final EntityPlayerMP player, final DynamicMachine machine, final EnumFacing facing,
                               final BlockPos anchor, final int dynamicSize, final boolean usingAE,
                               final boolean autoCraft, final boolean ignoreFluids, final boolean needAll,
                               final ItemStack tool) {
        final var world = player.getServerWorld();
        final var chunk = world.getChunkProvider().getLoadedChunk(anchor.getX() >> 4, anchor.getZ() >> 4);
        final var tile = chunk == null ? null : chunk.getTileEntity(anchor, Chunk.EnumCreateEntityType.CHECK);
        if (tile == null || tile.isInvalid() || ACTIVE.containsKey(player)) return;
        final var task = new Task(player, world, tile, anchor.toImmutable(), machine, facing, dynamicSize,
            usingAE, autoCraft, ignoreFluids, needAll, tool.copy());
        ACTIVE.put(player, task);
        QUEUE.addLast(task);
    }

    public static void tick(final MinecraftServer server) {
        ServerConstructionEffects.tick(server);
        if (server.getTickCounter() % 20 == 0) {
            for (final var player : server.getPlayerList().getPlayers()) {
                if (ACTIVE.containsKey(player)) continue;
                AssemblyRecovery.load(player);
                if (AssemblyRecovery.pending(player.getUniqueID())) {
                    final var materials = new AssemblyMaterials(player, false, false);
                    final int[] remaining = {64};
                    AssemblyRecovery.deliver(player, materials, () -> remaining[0]-- > 0);
                }
            }
        }
        if (QUEUE.isEmpty()) return;

        final long deadline = System.nanoTime() + NovaEngCoreConfig.MACHINE_ASSEMBLY_TOOL.buildBudgetMillis * 1_000_000L;
        final int tick = server.getTickCounter();
        while (!QUEUE.isEmpty()) {
            boolean progressed = false;
            for (int remaining = QUEUE.size(); remaining > 0; remaining--) {
                final long now = System.nanoTime();
                final long budget = deadline - now;
                if (budget <= 0 || QUEUE.isEmpty()) return;
                final var task = QUEUE.removeFirst();
                try {
                    progressed |= task.advance(tick, now + Math.max(1, budget / remaining));
                } catch (final AssemblyPattern.OutsideHeight failure) {
                    task.message("too_high", "y = " + failure.y);
                    task.stop();
                } catch (final AssemblyPattern.OutsideWorld failure) {
                    task.message("outside_world");
                    task.stop();
                } catch (final AssemblyPattern.TooLarge failure) {
                    task.message("too_large");
                    task.stop();
                } catch (final RuntimeException failure) {
                    NovaEngineeringCore.log.error("Construction for {} at {} failed", task.player.getName(), task.anchor, failure);
                    task.message("aborted");
                    task.stop();
                    PLACEMENT.flush(task.world);
                }
                if (task.finished) ACTIVE.remove(task.player, task);
                else QUEUE.addLast(task);
            }
            if (!progressed) return;
        }
    }

    public static void cancel(final EntityPlayerMP player) {
        final var task = ACTIVE.remove(player);
        if (task == null) return;
        QUEUE.remove(task);
        task.stop();
        PLACEMENT.flush(task.world);
    }

    public static void unload(final WorldServer world) {
        final var iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            final var task = iterator.next();
            if (task.world != world) continue;
            iterator.remove();
            task.stop();
        }
        QUEUE.removeIf(task -> task.world == world);
        PLACEMENT.flush(world);
    }

    public static void clear() {
        for (final var task : ACTIVE.values()) {
            task.finished = true;
            task.releaseController();
            PLACEMENT.flush(task.world);
        }
        ACTIVE.clear();
        QUEUE.clear();
        ServerConstructionEffects.stop();
        AssemblyRecovery.stop();
    }

    private enum Phase {EXPAND, INSPECT, RESERVE, GROUP, ORDER, PUBLISH, CLEAR_FLUIDS, DISMANTLE, BUILD_SOLIDS, VERIFY_SOLIDS, BUILD_FLUIDS}

    private enum Outcome {MATCHED, PLACED, MISSING, BLOCKED, UNLOADED, FLUID_SKIPPED, WAIT}

    private static final class Task {
        private final EntityPlayerMP player;
        private final WorldServer world;
        private final TileEntity anchorTile;
        private final BlockPos anchor;
        private final DynamicMachine machine;
        private final EnumFacing facing;
        private final @Nullable String blueprintId;
        private final boolean creative;
        private final ItemStack tool;
        private final boolean ignoreFluids;
        private final boolean needAll;
        private final boolean requireSpace;
        private final boolean waitMaterials;
        private final ItemStack miningProfile;
        private boolean dismantle;
        private boolean allMatched = true;
        private int inspectIndex;
        private int waitingTick = Integer.MIN_VALUE;
        private long retryRecoveryTick;
        private String waitingReason;
        private final IntArrayList clearFluids = new IntArrayList();
        private int clearIndex;
        private final long queuedAt = System.nanoTime();
        private final ServerConstructionEffects.Effect effect;
        private final AssemblyMaterials materials;
        private AssemblyController dismantlingController;
        private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        private final LongArrayList positions = new LongArrayList();
        private final ReferenceArrayList<AssemblyMaterials.Recipe> recipes = new ReferenceArrayList<>();
        private final IntArrayList solidChecks = new IntArrayList();
        private final IntArrayList links = new IntArrayList();
        private int[] heights = new int[256];
        private int[] tails;
        private int[] heads;
        private int[] workByGroup;
        private IBlockState[] preferred;
        private AssemblyPattern pattern;
        private Phase phase = Phase.EXPAND;
        private Chunk lastChunk;
        private int lastTick = -1;
        private int height;
        private int fluidBarrierTick = Integer.MIN_VALUE;
        private int next;
        private int stage;
        private int verifyIndex;
        private int admitted;
        private int resolved;
        private boolean hasFluidTargets;
        private int missing;
        private int blocked;
        private int unloaded;
        private int skippedFluids;
        private boolean solidGap;
        private boolean settle;
        private boolean finished;

        Task(final EntityPlayerMP player, final WorldServer world, final TileEntity anchorTile, final BlockPos anchor,
             final DynamicMachine machine, final EnumFacing facing, final int dynamicSize, final boolean usingAE,
             final boolean autoCraft, final boolean ignoreFluids, final boolean needAll, final ItemStack tool) {
            this.player = player;
            this.world = world;
            this.anchorTile = anchorTile;
            this.anchor = anchor;
            this.machine = machine;
            this.facing = facing;
            this.blueprintId = anchorTile instanceof TileMultiblockMachineController controller
                ? blueprintId(controller) : null;
            this.creative = player.isCreative();
            this.tool = tool;
            this.ignoreFluids = ignoreFluids;
            this.needAll = needAll;
            this.requireSpace = ItemMachineAssemblyTool.isRequireSpace(tool);
            this.waitMaterials = ItemMachineAssemblyTool.isWaitMaterials(tool);
            this.miningProfile = AssemblyMiningTool.profile(tool);
            this.dismantle = anchorTile instanceof TileMultiblockMachineController controller && controller.isStructureFormed();
            this.pattern = dismantle && anchorTile instanceof TileMultiblockMachineController controller && controller.getFoundPattern() != null
                ? new AssemblyPattern(controller.getFoundPattern(), anchor) : new AssemblyPattern(machine, facing, anchor, dynamicSize);
            this.materials = new AssemblyMaterials(player, usingAE, autoCraft);
            this.effect = ServerConstructionEffects.create(player, anchor);
            AssemblyRecovery.load(player);
        }

        boolean advance(final int tick, final long deadline) {
            if (lastTick != tick) {
                lastTick = tick;
                lastChunk = null;
                if (!valid()) {
                    stop();
                    PLACEMENT.flush(world);
                    return false;
                }
                materials.refresh();
            }
            if (waitingTick == tick || player.openContainer instanceof ContainerNEWCraftConfirm || !AssemblyPlacement.accepts(world) || building() && !effect.ready())
                return false;
            if (AssemblyRecovery.pending(player.getUniqueID())) {
                if (world.getTotalWorldTime() < retryRecoveryTick) return false;
                final var recovery = AssemblyRecovery.deliver(player, materials, () -> System.nanoTime() - deadline < 0);
                if (recovery == AssemblyRecovery.Result.FULL) {
                    retryRecoveryTick = world.getTotalWorldTime() + 20;
                    player.sendStatusMessage(new TextComponentTranslation("message.assembly.tip.recovery_full"), true);
                    return false;
                }
                if (recovery == AssemblyRecovery.Result.PENDING) return true;
            }
            boolean progressed = false;
            boolean complete = false;
            try {
                while (!finished && System.nanoTime() - deadline < 0) {
                    if ((phase == Phase.BUILD_FLUIDS || phase == Phase.VERIFY_SOLIDS) && tick == fluidBarrierTick)
                        break;
                    progressed = true;
                    switch (phase) {
                        case EXPAND -> expand();
                        case INSPECT -> inspect();
                        case RESERVE -> {
                            if (reserve()) break;
                            complete = true;
                        }
                        case GROUP -> {
                            if (effect.prepareChunkPlanStep(positions)) {
                                rewindHeights();
                                workByGroup = new int[effect.chunkPlan().size()];
                                heads = new int[STAGES];
                                tails = new int[STAGES];
                                phase = Phase.ORDER;
                            }
                        }
                        case ORDER -> order();
                        case PUBLISH -> {
                            if (!publish()) return true;
                        }
                        case CLEAR_FLUIDS -> clearWorldFluids();
                        case DISMANTLE -> {
                            if (!dismantleStep()) return progressed;
                        }
                        case BUILD_SOLIDS -> {
                            if (!buildSolids()) return progressed;
                        }
                        case VERIFY_SOLIDS -> verifySolids();
                        case BUILD_FLUIDS -> {
                            if (stage == STAGES) complete = true;
                            else if (!buildFluids()) return progressed;
                        }
                    }
                    if (finished || complete || player.openContainer instanceof ContainerNEWCraftConfirm) break;
                    if (AssemblyRecovery.pending(player.getUniqueID())) {
                        waitingTick = tick;
                        break;
                    }
                }
            } finally {
                syncStatus();
                if (building()) {
                    PLACEMENT.flush(world);
                    if (settle) {
                        settle = false;
                        effect.flushSettled();
                    }
                }
            }
            if (complete && !finished) finish();
            return progressed;
        }

        @SuppressWarnings("deprecation")
        private boolean valid() {
            if (player.world != world || !player.isEntityAlive() || player.isSpectator()
                || player.isCreative() != creative || anchorTile.isInvalid()) return false;
            final var chunk = chunk(anchor);
            if (chunk == null || chunk.getTileEntity(anchor, Chunk.EnumCreateEntityType.CHECK) != anchorTile)
                return false;
            final var state = chunk.getBlockState(anchor);
            if (anchorTile instanceof TileMultiblockMachineController controller) {
                if (!(state.getBlock() instanceof BlockController block)
                    || state.getValue(BlockController.FACING) != facing
                    || !Objects.equals(blueprintId, blueprintId(controller))) return false;
                return blueprintId != null || block.getParentMachine() == machine;
            }
            final var block = state.getBlock();
            final var actual = block.getActualState(state, world, anchor);
            if (NEWMachineAssemblyManager.getDynamicMachine(actual) != machine) return false;
            final var property = block.getBlockState().getProperty("facing");
            final Object value = property == null ? null : actual.getValue(property);
            if (!(value instanceof EnumFacing direction)) return true;
            return (direction.getAxis().isVertical() ? EnumFacing.NORTH : direction) == facing;
        }

        @Nullable
        private static String blueprintId(final TileMultiblockMachineController controller) {
            final var blueprint = controller.getInventory().getStackInSlot(TileMultiblockMachineController.BLUEPRINT_SLOT);
            final var tag = blueprint.getTagCompound();
            return blueprint.isEmpty() || tag == null || !tag.hasKey(ItemBlueprint.DYNAMIC_MACHINE_NBT_KEY)
                ? null : tag.getString(ItemBlueprint.DYNAMIC_MACHINE_NBT_KEY);
        }

        @Nullable
        private Chunk chunk(final BlockPos pos) {
            final int x = pos.getX() >> 4;
            final int z = pos.getZ() >> 4;
            final var cached = lastChunk;
            if (cached != null && cached.x == x && cached.z == z && cached.isLoaded()) return cached;
            final var chunk = world.getChunkProvider().getLoadedChunk(x, z);
            if (chunk == null || chunk.isEmpty()) return null;
            lastChunk = chunk;
            return chunk;
        }

        private BlockPos.MutableBlockPos position(final long packed) {
            return probe.setPos((int) (packed >> 38), (int) (packed << 26 >> 52), (int) (packed << 38 >> 38));
        }

        private void expand() {
            if (pattern.step()) {
                pattern = null;
                if (!(anchorTile instanceof TileMultiblockMachineController))
                    dismantle = allMatched && !positions.isEmpty();
                preferred = new IBlockState[positions.size()];
                phase = Phase.INSPECT;
                return;
            }
            final var info = pattern.information;
            if (info == null) return;
            final var pos = position(pattern.position);
            effect.includeInArea(pos);
            if (pos.equals(anchor)) return;
            final var recipe = materials.recipe(info);
            positions.add(pattern.position);
            recipes.add(recipe);
            links.add(0);
            if (!(anchorTile instanceof TileMultiblockMachineController) && allMatched) {
                final var chunk = chunk(pos);
                allMatched = chunk != null && recipe.matches(world, pos, chunk.getBlockState(pos));
            }
        }

        private boolean space(final BlockPos pos, final IBlockState state) {
            return AssemblyHarvest.air(world, pos, state) || (requireSpace ? ignoreFluids && AssemblyPlacement.fluid(state)
                : AssemblyHarvest.canHarvest(player, world, pos, state, miningProfile));
        }

        private void inspect() {
            if (inspectIndex == positions.size()) {
                if (!dismantle && requireSpace && blocked + unloaded != 0) {
                    message("require_space", blocked, unloaded);
                    stop();
                    return;
                }
                materials.beginPreflight();
                rewindHeights();
                phase = Phase.RESERVE;
                return;
            }
            final int index = inspectIndex++;
            final var pos = position(positions.getLong(index));
            final var recipe = recipes.get(index);
            final var chunk = chunk(pos);
            if (chunk == null) {
                if (dismantle && recipe.sample() != null) {
                    preferred[index] = recipe.sample();
                    final int y = pos.getY();
                    links.set(index, heights[y]);
                    heights[y] = index + 1;
                } else skip(Outcome.UNLOADED, recipe.fluidOnly());
                return;
            }
            final var state = chunk.getBlockState(pos);
            if (dismantle ? AssemblyHarvest.air(world, pos, state) : recipe.matches(world, pos, state)) {
                if (!dismantle && !AssemblyPlacement.fluid(state)) solidChecks.add(index);
                return;
            }
            if (!dismantle && (!AssemblyPlacement.authorized(player, world, pos, tool) || !space(pos, state))) {
                skip(Outcome.BLOCKED, recipe.fluidOnly());
                return;
            }
            if (dismantle) preferred[index] = state;
            else if (!requireSpace && AssemblyPlacement.fluid(state)) clearFluids.add(index);
            final int y = pos.getY();
            links.set(index, heights[y]);
            heights[y] = index + 1;
        }

        private void rewindHeights() {
            height = 0;
            next = heights[0];
        }

        private int nextByHeight() {
            if (height >= heights.length) return -1;
            while (next == 0) {
                if (++height >= heights.length) return -1;
                next = heights[height];
            }
            final int index = next - 1;
            next = links.getInt(index);
            return index;
        }

        private boolean reserve() {
            final int index = nextByHeight();
            if (index < 0) {
                if (!materials.finishPreflight()) return true;
                if (!dismantle && needAll && missing != 0) {
                    message("require_all", missing);
                    stop();
                }
                if (finished || admitted == 0) return false;
                if (waitMaterials) missing = 0;
                phase = Phase.GROUP;
                return true;
            }
            final var recipe = recipes.get(index);
            final boolean available = dismantle || materials.reserve(recipe);
            if (available || waitMaterials) {
                final var state = dismantle ? preferred[index] : available ? materials.reservedState() : recipe.waitingState();
                if (state == null) {
                    message("material_unavailable", 1);
                    stop();
                    return true;
                }
                preferred[index] = state;
                final boolean fluid = AssemblyPlacement.fluid(state);
                if (fluid) hasFluidTargets = true;
                else if (!dismantle) solidChecks.add(index);
                admitted++;
            } else {
                skip(Outcome.MISSING, recipe.fluidOnly());
            }
            return true;
        }

        private void order() {
            final int index = nextByHeight();
            if (index < 0) {
                heights = tails = null;
                effect.planFades(workByGroup);
                workByGroup = null;
                next = 0;
                phase = Phase.PUBLISH;
                return;
            }
            final var recipe = recipes.get(index);
            if (recipe == null || preferred[index] == null) return;
            final int group = effect.chunkPlan().chunkAt(index);
            workByGroup[group]++;
            final var state = preferred[index];
            final boolean fluid = AssemblyPlacement.fluid(state);
            final boolean fast = !fluid && recipe.staticOnly();
            final int slot = dismantle ? (fluid ? 0 : GROUPS) + GROUPS - 1 - group
                : fluid ? SOLID_SLOTS + group : group * 2 + (fast ? 0 : 1);
            final int id = index + 1;
            // Prepend static cells so each column gets its topmost block first. Other cells keep support order.
            if (dismantle || fast) {
                links.set(index, heads[slot]);
                heads[slot] = id;
            } else {
                final int tail = tails[slot];
                if (tail == 0) heads[slot] = id;
                else links.set(tail - 1, id);
                tails[slot] = id;
                links.set(index, 0);
            }
        }

        private boolean publish() {
            while (next == 0 && stage < STAGES) next = heads[stage++];
            if (next == 0) {
                stage = 0;
                if (dismantle && anchorTile instanceof TileMultiblockMachineController) {
                    final var controller = (AssemblyController) anchorTile;
                    if (!controller.nova$beginDismantling()) {
                        message("already_assembly");
                        stop();
                        return false;
                    }
                    dismantlingController = controller;
                    if (finished) {
                        releaseController();
                        return false;
                    }
                }
                syncStatus();
                effect.start(dismantle);
                phase = dismantle ? Phase.DISMANTLE : Phase.CLEAR_FLUIDS;
                message(dismantle ? "dismantle_start" : "already_assembly.start");
                return false;
            }
            final int index = next - 1;
            next = links.getInt(index);
            effect.add(position(positions.getLong(index)), AssemblyPlacement.fluid(preferred[index]), effect.chunkPlan().chunkAt(index));
            return true;
        }

        private boolean building() {
            return phase == Phase.CLEAR_FLUIDS || phase == Phase.DISMANTLE || phase == Phase.BUILD_SOLIDS
                || phase == Phase.VERIFY_SOLIDS || phase == Phase.BUILD_FLUIDS;
        }

        private boolean waitFor(final String reason) {
            waitingTick = lastTick;
            if (!reason.equals(waitingReason)) {
                player.sendStatusMessage(new TextComponentTranslation("message.assembly.tip."
                    + (reason.equals("waiting_material") ? reason : "blocked_wait")), true);
                waitingReason = reason;
            }
            return false;
        }

        private void clearWorldFluids() {
            if (clearIndex == clearFluids.size()) {
                phase = Phase.BUILD_SOLIDS;
                return;
            }
            final int index = clearFluids.getInt(clearIndex++);
            if (preferred[index] == null) return;
            final var pos = position(positions.getLong(index));
            var state = preferred[index];
            Outcome failure = null;
            try {
                final var chunk = chunk(pos);
                if (chunk == null) {
                    reportHarvest(pos, state, AssemblyHarvest.Result.CHUNK_UNLOADED);
                    failure = Outcome.UNLOADED;
                } else {
                    state = chunk.getBlockState(pos);
                    if (AssemblyPlacement.fluid(state)) {
                        PLACEMENT.flush(world);
                        final var result = harvest(pos, chunk, state);
                        if (result != AssemblyHarvest.Result.SUCCESS) failure = harvestFailure(result);
                    }
                }
            } catch (RuntimeException exception) {
                reportException(pos, state, exception);
                failure = Outcome.BLOCKED;
            }
            if (failure != null) resolveTarget(index, pos, failure, AssemblyPlacement.fluid(preferred[index]));
        }

        private boolean dismantleStep() {
            while (stage < SOLID_SLOTS && heads[stage] == 0) stage++;
            if (stage == SOLID_SLOTS) {
                finish();
                return false;
            }
            final int index = heads[stage] - 1;
            final var pos = position(positions.getLong(index));
            var state = preferred[index];
            Outcome outcome;
            try {
                final var chunk = chunk(pos);
                if (chunk == null) {
                    reportHarvest(pos, state, AssemblyHarvest.Result.CHUNK_UNLOADED);
                    outcome = Outcome.UNLOADED;
                } else {
                    state = chunk.getBlockState(pos);
                    if (AssemblyHarvest.air(world, pos, state)) outcome = Outcome.MATCHED;
                    else {
                        PLACEMENT.flush(world);
                        final var result = harvest(pos, chunk, state);
                        outcome = result == AssemblyHarvest.Result.SUCCESS ? Outcome.PLACED : harvestFailure(result);
                    }
                }
            } catch (RuntimeException exception) {
                reportException(pos, state, exception);
                outcome = Outcome.BLOCKED;
            }
            heads[stage] = links.getInt(index);
            resolveTarget(index, pos, outcome, AssemblyPlacement.fluid(preferred[index]));
            return true;
        }

        private AssemblyHarvest.Result harvest(final BlockPos pos, final Chunk chunk, final IBlockState state) {
            try {
                final var result = AssemblyHarvest.remove(player, world, chunk, pos.toImmutable(), tool, miningProfile, state, materials);
                if (result != AssemblyHarvest.Result.SUCCESS) reportHarvest(pos, state, result);
                return result;
            } catch (RuntimeException exception) {
                reportException(pos, state, exception);
                return AssemblyHarvest.Result.CALLBACK_FAILED;
            }
        }

        private static Outcome harvestFailure(final AssemblyHarvest.Result result) {
            return result == AssemblyHarvest.Result.CHUNK_UNLOADED ? Outcome.UNLOADED : Outcome.BLOCKED;
        }

        @SuppressWarnings("deprecation")
        private void reportHarvest(final BlockPos pos, final IBlockState state, final AssemblyHarvest.Result result) {
            final ITextComponent reason;
            if (result == AssemblyHarvest.Result.TOOL_REQUIRED) {
                final var actual = state.getBlock().getActualState(state, world, pos);
                final var toolClass = actual.getBlock().getHarvestTool(actual);
                final var toolName = miningProfile.isEmpty() ? new TextComponentTranslation("message.assembly.harvest.default_tool")
                    : miningProfile.getTextComponent();
                reason = new TextComponentTranslation(result.translationKey, toolClass == null ? "-" : toolClass,
                    actual.getBlock().getHarvestLevel(actual), toolName);
            } else reason = new TextComponentTranslation(result.translationKey);
            reportFailure(pos, state, reason);
        }

        private void reportException(final BlockPos pos, final IBlockState state, final RuntimeException exception) {
            final var detail = exception.getMessage();
            final String type = exception.getClass().getName();
            reportFailure(pos, state, new TextComponentTranslation("message.assembly.harvest.exception",
                detail == null || detail.isBlank() ? type : type + ": " + detail));
            NovaEngineeringCore.log.error("Skipping {} block {} in dimension {} at ({}, {}, {})",
                dismantle ? "dismantling" : "clearing", state.getBlock().getRegistryName(), world.provider.getDimension(),
                pos.getX(), pos.getY(), pos.getZ(), exception);
        }

        private void reportFailure(final BlockPos pos, final IBlockState state, final ITextComponent reason) {
            final var message = new TextComponentTranslation(dismantle ? "message.assembly.tip.dismantle_failed" : "message.assembly.tip.clear_failed",
                world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), state.getBlock().getRegistryName(), reason);
            message.getStyle().setColor(TextFormatting.RED);
            player.sendMessage(message);
        }

        private void resolveTarget(final int index, final BlockPos pos, final Outcome outcome, final boolean fluid) {
            preferred[index] = null;
            resolved++;
            skip(outcome, fluid);
            settle |= effect.resolved(pos, fluid, outcome == Outcome.PLACED);
        }

        private void syncStatus() {
            effect.status(Math.max(0, admitted - resolved) + missing, blocked + skippedFluids, unloaded);
        }

        private boolean buildSolids() {
            while (stage < SOLID_SLOTS && heads[stage] == 0) stage++;
            if (stage == SOLID_SLOTS) {
                PLACEMENT.flush(world);
                if (settle) {
                    settle = false;
                    effect.flushSettled();
                }
                fluidBarrierTick = lastTick;
                phase = hasFluidTargets ? Phase.VERIFY_SOLIDS : Phase.BUILD_FLUIDS;
                stage = SOLID_SLOTS;
                return true;
            }
            return buildOne(false);
        }

        private void verifySolids() {
            if (verifyIndex == solidChecks.size()) {
                stage = SOLID_SLOTS;
                phase = Phase.BUILD_FLUIDS;
                return;
            }
            final int index = solidChecks.getInt(verifyIndex++);
            final var pos = position(positions.getLong(index));
            final var chunk = chunk(pos);
            if (chunk == null || !recipes.get(index).matches(world, pos, chunk.getBlockState(pos))) solidGap = true;
        }

        private boolean buildFluids() {
            while (stage < STAGES && heads[stage] == 0) stage++;
            return stage == STAGES || buildOne(true);
        }

        private boolean buildOne(final boolean fluid) {
            final int index = heads[stage] - 1;
            if (preferred[index] == null) {
                heads[stage] = links.getInt(index);
                return true;
            }
            final var pos = position(positions.getLong(index));
            final var outcome = place(pos, recipes.get(index), preferred[index], fluid);
            if (outcome == Outcome.WAIT) return false;
            heads[stage] = links.getInt(index);
            waitingReason = null;
            resolveTarget(index, pos, outcome, fluid);
            if (heads[stage] == 0 && (fluid || (stage & 1) == 1 || heads[stage + 1] == 0)) PLACEMENT.flush(world);
            return true;
        }

        private Outcome retry(final Outcome failure) {
            if (requireSpace || failure == Outcome.MISSING && (waitMaterials || needAll)) {
                waitFor(failure == Outcome.MISSING ? "waiting_material" : "blocked");
                return Outcome.WAIT;
            }
            return failure;
        }

        private Outcome place(final BlockPos pos, final AssemblyMaterials.Recipe recipe,
                              final IBlockState preferred, final boolean fluid) {
            final var chunk = chunk(pos);
            if (chunk == null) return retry(Outcome.UNLOADED);
            var before = chunk.getBlockState(pos);
            if (recipe.matches(world, pos, before)) return Outcome.MATCHED;
            if (fluid && solidGap) return Outcome.FLUID_SKIPPED;
            if (!AssemblyPlacement.authorized(player, world, pos, tool) || !space(pos, before))
                return retry(Outcome.BLOCKED);
            boolean committed = false;
            try {
                final var state = materials.take(recipe, preferred);
                if (state == null) {
                    if (waitMaterials || needAll) {
                        waitFor("waiting_material");
                        return Outcome.WAIT;
                    }
                    return Outcome.MISSING;
                }
                if (!requireSpace && !AssemblyHarvest.air(world, pos, before)) {
                    PLACEMENT.flush(world);
                    final var result = harvest(pos, chunk, before);
                    if (result != AssemblyHarvest.Result.SUCCESS) return harvestFailure(result);
                    before = chunk.getBlockState(pos);
                    if (!AssemblyHarvest.air(world, pos, before)) return retry(Outcome.BLOCKED);
                }
                if (!finished) committed = before.equals(state)
                    || PLACEMENT.place(player, world, chunk, pos.toImmutable(), tool, before, state);
                return committed ? Outcome.PLACED : retry(Outcome.BLOCKED);
            } finally {
                if (committed) materials.commit();
                else materials.refund();
            }
        }

        private void skip(final Outcome outcome, final boolean fluid) {
            switch (outcome) {
                case MATCHED, PLACED, WAIT -> {
                    return;
                }
                case MISSING -> missing++;
                case BLOCKED -> blocked++;
                case UNLOADED -> unloaded++;
                case FLUID_SKIPPED -> skippedFluids++;
            }
            solidGap |= !fluid;
        }

        private void finish() {
            finished = true;
            releaseController();
            final boolean success = missing + blocked + unloaded + skippedFluids == 0;
            syncStatus();
            effect.finish(success);
            final String elapsed = String.format(Locale.ROOT, "%.2f", (System.nanoTime() - queuedAt) / 1_000_000_000.0);
            if (success) message(dismantle ? "dismantle_complete" : "complete", elapsed);
            else if (dismantle) message("dismantle_partial", elapsed, blocked, unloaded);
            else message("partial", elapsed, missing, blocked, unloaded, skippedFluids);
        }

        private void stop() {
            finished = true;
            releaseController();
            effect.cancel();
        }

        private void releaseController() {
            final var controller = dismantlingController;
            if (controller == null) return;
            dismantlingController = null;
            controller.nova$endDismantling();
        }

        private void message(final String key, final Object... arguments) {
            player.sendMessage(new TextComponentTranslation("message.assembly.tip." + key, arguments));
        }
    }
}
