package github.kasuminova.novaeng.client.util;

import github.kasuminova.novaeng.common.util.BlockArrayPreviewRenderUtils;
import hellfirepvp.modularmachinery.client.util.BlockArrayPreviewRenderHelper;
import hellfirepvp.modularmachinery.client.util.DynamicMachineRenderContext;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.List;

public final class NEWBlockArrayPreviewRenderHelper extends BlockArrayPreviewRenderHelper {
    public static final NEWBlockArrayPreviewRenderHelper INSTANCE = new NEWBlockArrayPreviewRenderHelper();
    private final BlockArrayPreviewRenderUtils utils = (BlockArrayPreviewRenderUtils) (Object) this;
    private PreviewState previewState;
    private boolean work;
    private long key;

    private NEWBlockArrayPreviewRenderHelper() {
    }

    public BlockArrayPreviewRenderUtils getUtils() {
        return this.utils;
    }

    public boolean getWork() {
        return this.work;
    }

    public void setWork(final boolean work) {
        this.work = work;
    }

    public long getKey() {
        return this.key;
    }

    public void setKey(final long key) {
        this.key = key;
    }

    public boolean startPreview(final DynamicMachineRenderContext context, final BlockPos pos, final EnumFacing facing) {
        if (!super.startPreview(context)) {
            return false;
        }
        final var world = Minecraft.getMinecraft().world;
        if (world == null) {
            super.unloadWorld();
            return false;
        }
        final var rotate = facing == null ? world.getBlockState(pos).getValue(BlockController.FACING) : facing;
        final var direction = MiscUtils.rotateYCCWNorthUntil(new BlockPos(this.utils.getRenderHelperOffset()), rotate);
        final var attached = pos.subtract(direction);
        final var array = MiscUtils.rotateYCCWNorthUntil(this.utils.getMatchArray(), rotate);
        this.utils.setMatchArray(array);
        this.utils.setAttachedPosition(attached);
        this.utils.setFacing(facing);
        this.utils.setForceDetachedPreview(false);
        this.key = pos.toLong();
        this.previewState = this.buildPreviewState(context, array, attached);
        this.applyRenderedSelection();
        this.work = true;
        return true;
    }

    public int getLayers() {
        final var state = this.previewState;
        if (state == null) {
            return 0;
        }
        if (state.manualSelectionIndex != null) {
            return state.manualSelectionIndex;
        }
        for (int i = 0; i < state.layerOrder.length; i++) {
            if (state.layerOrder[i] == state.autoVisibleLayer) {
                return i;
            }
        }
        return state.fullViewIndex;
    }

    public void setLayers(final int value) {
        final var state = this.previewState;
        if (state == null) {
            return;
        }
        state.manualSelectionIndex = Math.floorMod(value, state.fullViewIndex + 1);
        this.applyRenderedSelection();
    }

    @Override
    public void tick() {
        final var state = this.previewState;
        final var mc = Minecraft.getMinecraft();
        if (state == null || mc.player == null) {
            return;
        }
        if (mc.player.getDistanceSqToCenter(state.attachedPosition) >= 1024.0) {
            this.unloadWorld();
            return;
        }
        final var world = mc.world;
        if (world == null || this.utils.getRenderHelper() == null) {
            return;
        }
        if (state.confirmedLayerIndex >= 0 && !this.auditConfirmedLayer(world, state)) {
            state.confirmedLayerIndex = this.recomputeConfirmedLayerIndex(world, state);
            state.auditLayerIndex = 0;
        }
        final int next = state.confirmedLayerIndex + 1;
        if (next >= state.layerOrder.length) {
            this.unloadWorld();
            return;
        }
        if (this.doesLayerMatch(world, state, next)) {
            state.confirmedLayerIndex = next;
            state.auditLayerIndex = 0;
            if (state.confirmedLayerIndex + 1 >= state.layerOrder.length) {
                this.unloadWorld();
                return;
            }
        }
        final int layer = state.layerOrder[state.confirmedLayerIndex + 1];
        if (state.autoVisibleLayer != layer) {
            state.autoVisibleLayer = layer;
            if (state.manualSelectionIndex == null) {
                this.applyRenderedSelection();
            }
        }
    }

    @Override
    public void unloadWorld() {
        this.utils.setForceDetachedPreview(false);
        super.unloadWorld();
    }

    public void renderTranslucentBlocks() {
        this.utils.n$renderTranslucentBlocks();
    }

    public void clear() {
        final boolean previous = this.work;
        this.previewState = null;
        this.utils.setForceDetachedPreview(false);
        this.work = false;
        this.key = 0;
        if (previous && Minecraft.getMinecraft().player != null) {
            Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("gui.blueprint.popout.placed.dynamic_pattern.unload"));
        }
    }

    private PreviewState buildPreviewState(final DynamicMachineRenderContext context, final BlockArray array, final BlockPos position) {
        final Int2ObjectMap<List<LayerEntry>> layers = new Int2ObjectAVLTreeMap<>();
        for (final var entry : array.getPattern().entrySet()) {
            layers.computeIfAbsent(entry.getKey().getY(), ignored -> new ObjectArrayList<>())
                  .add(new LayerEntry(entry.getKey(), entry.getValue()));
        }
        final var state = new PreviewState(context, position, layers.keySet().toIntArray(), layers);
        state.confirmedLayerIndex = this.recomputeConfirmedLayerIndex(Minecraft.getMinecraft().world, state);
        final int next = Math.min(state.confirmedLayerIndex + 1, state.layerOrder.length - 1);
        if (next >= 0 && state.layerOrder.length != 0) {
            state.autoVisibleLayer = state.layerOrder[next];
        }
        return state;
    }

    private void applyRenderedSelection() {
        final var state = this.previewState;
        if (state == null) {
            return;
        }
        if (state.manualSelectionIndex != null && state.manualSelectionIndex == state.fullViewIndex) {
            this.utils.setForceDetachedPreview(true);
            return;
        }
        this.utils.setForceDetachedPreview(false);
        final int layer = state.manualSelectionIndex == null ? state.autoVisibleLayer : state.layerOrder[state.manualSelectionIndex];
        if (this.utils.getRenderedLayer() != layer) {
            this.utils.setRenderedLayer(layer);
        }
    }

    private boolean auditConfirmedLayer(final World world, final PreviewState state) {
        if (state.confirmedLayerIndex < 0) {
            return true;
        }
        if (state.auditLayerIndex > state.confirmedLayerIndex) {
            state.auditLayerIndex = 0;
        }
        final boolean matches = this.doesLayerMatch(world, state, state.auditLayerIndex);
        state.auditLayerIndex = state.confirmedLayerIndex <= 0 ? 0 : (state.auditLayerIndex + 1) % (state.confirmedLayerIndex + 1);
        return matches;
    }

    private int recomputeConfirmedLayerIndex(final World world, final PreviewState state) {
        if (world == null) {
            return -1;
        }
        for (int i = 0; i < state.layerOrder.length; i++) {
            if (!this.doesLayerMatch(world, state, i)) {
                return i - 1;
            }
        }
        return state.layerOrder.length - 1;
    }

    private boolean doesLayerMatch(final World world, final PreviewState state, final int index) {
        if (index < 0 || index >= state.layerOrder.length) {
            return true;
        }
        final var entries = state.layerEntries.get(state.layerOrder[index]);
        if (entries == null) {
            return true;
        }
        final var replacements = state.context.getDisplayedMachine().getModifiersAsMatchingReplacements();
        entryLoop:
        for (final var entry : entries) {
            final var pos = state.attachedPosition.add(entry.pos());
            if (entry.info().matches(world, pos, false)) {
                continue;
            }
            final var alternatives = replacements.get(entry.pos());
            if (alternatives != null) {
                for (final var replacement : alternatives) {
                    if (replacement.matches(world, pos, false)) {
                        continue entryLoop;
                    }
                }
            }
            return false;
        }
        return true;
    }

    private record LayerEntry(BlockPos pos, BlockArray.BlockInformation info) {
    }

    private static final class PreviewState {
        final DynamicMachineRenderContext context;
        final BlockPos attachedPosition;
        final int[] layerOrder;
        final Int2ObjectMap<List<LayerEntry>> layerEntries;
        final int fullViewIndex;
        int confirmedLayerIndex = -1;
        int auditLayerIndex;
        int autoVisibleLayer;
        Integer manualSelectionIndex;

        PreviewState(final DynamicMachineRenderContext context, final BlockPos position, final int[] order,
                     final Int2ObjectMap<List<LayerEntry>> entries) {
            this.context = context;
            this.attachedPosition = position;
            this.layerOrder = order;
            this.layerEntries = entries;
            this.fullViewIndex = order.length;
            this.autoVisibleLayer = order.length == 0 ? 0 : order[0];
        }
    }
}
