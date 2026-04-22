package github.kasuminova.novaeng.client.util

import github.kasuminova.novaeng.mixin.util.BlockArrayPreviewRenderUtils
import hellfirepvp.modularmachinery.client.util.BlockArrayPreviewRenderHelper
import hellfirepvp.modularmachinery.client.util.DynamicMachineRenderContext
import hellfirepvp.modularmachinery.common.block.BlockController
import hellfirepvp.modularmachinery.common.util.BlockArray
import hellfirepvp.modularmachinery.common.util.MiscUtils
import net.minecraft.client.Minecraft
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentTranslation

@Suppress("CAST_NEVER_SUCCEEDS")
object NEWBlockArrayPreviewRenderHelper : BlockArrayPreviewRenderHelper() {

    private data class LayerEntry(
        val pos: BlockPos,
        val info: BlockArray.BlockInformation
    )

    private class PreviewState(
        val context: DynamicMachineRenderContext,
        val attachedPosition: BlockPos,
        val layerOrder: IntArray,
        val layerEntries: Map<Int, List<LayerEntry>>,
        val fullViewIndex: Int,
        var confirmedLayerIndex: Int,
        var auditLayerIndex: Int,
        var autoVisibleLayer: Int,
        var manualSelectionIndex: Int?
    )

    val utils = this as BlockArrayPreviewRenderUtils
    private var previewState: PreviewState? = null
    var work = false
    var key = 0L

    fun startPreview(currentContext: DynamicMachineRenderContext, pos: BlockPos, facing: EnumFacing?): Boolean {
        if (!super.startPreview(currentContext)) {
            return false
        }

        val world = Minecraft.getMinecraft().world ?: run {
            super.unloadWorld()
            return false
        }
        val lookState = world.getBlockState(pos)
        val rotate = facing ?: lookState.getValue(BlockController.FACING)
        val moveDir = MiscUtils.rotateYCCWNorthUntil(BlockPos(utils.renderHelperOffset), rotate)
        val attachedPosition = pos.subtract(moveDir)
        val rotatedArray = MiscUtils.rotateYCCWNorthUntil(utils.matchArray, rotate)

        utils.matchArray = rotatedArray
        utils.attachedPosition = attachedPosition
        utils.facing = facing
        utils.setForceDetachedPreview(false)
        key = pos.toLong()

        previewState = buildPreviewState(currentContext, rotatedArray, attachedPosition)
        applyRenderedSelection()
        work = true
        return true
    }

    fun setLayers(newValue: Int) {
        val state = previewState ?: return
        val size = state.fullViewIndex + 1
        val value = Math.floorMod(newValue, size)
        state.manualSelectionIndex = value
        applyRenderedSelection()
    }

    fun getLayers(): Int {
        val state = previewState ?: return 0
        return state.manualSelectionIndex ?: state.layerOrder.indexOf(state.autoVisibleLayer).let {
            if (it >= 0) it else state.fullViewIndex
        }
    }

    override fun tick() {
        val state = previewState ?: return
        val mc = Minecraft.getMinecraft()
        val player = mc.player ?: return
        if (player.getDistanceSqToCenter(state.attachedPosition) >= 1024.0) {
            unloadWorld()
            return
        }

        val world = mc.world ?: return
        if (utils.renderHelper == null) {
            return
        }

        if (state.confirmedLayerIndex >= 0 && !auditConfirmedLayer(world, state)) {
            state.confirmedLayerIndex = recomputeConfirmedLayerIndex(world, state)
            state.auditLayerIndex = 0
        }

        val nextLayerIndex = state.confirmedLayerIndex + 1
        if (nextLayerIndex >= state.layerOrder.size) {
            unloadWorld()
            return
        }

        if (doesLayerMatch(world, state, nextLayerIndex)) {
            state.confirmedLayerIndex = nextLayerIndex
            state.auditLayerIndex = 0

            val remainingLayerIndex = state.confirmedLayerIndex + 1
            if (remainingLayerIndex >= state.layerOrder.size) {
                unloadWorld()
                return
            }
        }

        val autoLayer = state.layerOrder[state.confirmedLayerIndex + 1]
        if (state.autoVisibleLayer != autoLayer) {
            state.autoVisibleLayer = autoLayer
            if (state.manualSelectionIndex == null) {
                applyRenderedSelection()
            }
        }
    }

    override fun unloadWorld() {
        utils.setForceDetachedPreview(false)
        super.unloadWorld()
    }

    fun renderTranslucentBlocks() {
        utils.`n$renderTranslucentBlocks`()
    }

    fun clear() {
        val hadWork = work
        previewState = null
        utils.setForceDetachedPreview(false)
        work = false
        key = 0

        if (hadWork && Minecraft.getMinecraft().player != null) {
            Minecraft.getMinecraft().player.sendMessage(
                TextComponentTranslation(
                    "gui.blueprint.popout.placed.dynamic_pattern.unload"
                )
            )
        }
    }

    private fun buildPreviewState(
        context: DynamicMachineRenderContext,
        rotatedArray: BlockArray,
        attachedPosition: BlockPos
    ): PreviewState {
        val groupedLayers = rotatedArray.pattern.entries
            .groupBy({ it.key.y }) { LayerEntry(it.key, it.value) }
            .toSortedMap()
        val layerOrder = groupedLayers.keys.toIntArray()
        val initialAutoLayer = layerOrder.firstOrNull() ?: 0
        return PreviewState(
            context = context,
            attachedPosition = attachedPosition,
            layerOrder = layerOrder,
            layerEntries = groupedLayers,
            fullViewIndex = layerOrder.size,
            confirmedLayerIndex = -1,
            auditLayerIndex = 0,
            autoVisibleLayer = initialAutoLayer,
            manualSelectionIndex = null
        ).also {
            it.confirmedLayerIndex = recomputeConfirmedLayerIndex(Minecraft.getMinecraft().world, it)
            val nextLayerIndex = (it.confirmedLayerIndex + 1).coerceAtMost(it.layerOrder.lastIndex)
            if (nextLayerIndex >= 0 && it.layerOrder.isNotEmpty()) {
                it.autoVisibleLayer = it.layerOrder[nextLayerIndex]
            }
        }
    }

    private fun applyRenderedSelection() {
        val state = previewState ?: return
        val selectionIndex = state.manualSelectionIndex
        if (selectionIndex == state.fullViewIndex) {
            utils.setForceDetachedPreview(true)
            return
        }

        utils.setForceDetachedPreview(false)
        val layer = if (selectionIndex == null) {
            state.autoVisibleLayer
        } else {
            state.layerOrder[selectionIndex]
        }
        if (utils.renderedLayer != layer) {
            utils.renderedLayer = layer
        }
    }

    private fun auditConfirmedLayer(world: net.minecraft.world.World, state: PreviewState): Boolean {
        if (state.confirmedLayerIndex < 0) {
            return true
        }
        if (state.auditLayerIndex > state.confirmedLayerIndex) {
            state.auditLayerIndex = 0
        }
        val matches = doesLayerMatch(world, state, state.auditLayerIndex)
        state.auditLayerIndex = if (state.confirmedLayerIndex <= 0) {
            0
        } else {
            (state.auditLayerIndex + 1) % (state.confirmedLayerIndex + 1)
        }
        return matches
    }

    private fun recomputeConfirmedLayerIndex(
        world: net.minecraft.world.World?,
        state: PreviewState
    ): Int {
        if (world == null) {
            return -1
        }
        for (layerIndex in state.layerOrder.indices) {
            if (!doesLayerMatch(world, state, layerIndex)) {
                return layerIndex - 1
            }
        }
        return state.layerOrder.lastIndex
    }

    private fun doesLayerMatch(
        world: net.minecraft.world.World,
        state: PreviewState,
        layerIndex: Int
    ): Boolean {
        if (layerIndex !in state.layerOrder.indices) {
            return true
        }
        val layer = state.layerOrder[layerIndex]
        val layerEntries = state.layerEntries[layer].orEmpty()
        val replacements = state.context.displayedMachine.modifiersAsMatchingReplacements
        for (entry in layerEntries) {
            val absolutePos = state.attachedPosition.add(entry.pos)
            if (entry.info.matches(world, absolutePos, false)) {
                continue
            }
            val replacement = replacements[entry.pos]
            if (replacement != null && replacement.any { it.matches(world, absolutePos, false) }) {
                continue
            }
            return false
        }
        return true
    }
}
