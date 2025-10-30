package github.kasuminova.novaeng.client.util

import github.kasuminova.novaeng.mixin.util.BlockArrayPreviewRenderUtils
import github.kasuminova.novaeng.mixin.util.BlockArrayRenderUtils
import hellfirepvp.modularmachinery.client.util.BlockArrayPreviewRenderHelper
import hellfirepvp.modularmachinery.client.util.DynamicMachineRenderContext
import hellfirepvp.modularmachinery.common.block.BlockController
import hellfirepvp.modularmachinery.common.util.MiscUtils
import net.minecraft.client.Minecraft
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentTranslation
import java.util.stream.IntStream

@Suppress("CAST_NEVER_SUCCEEDS")
object NEWBlockArrayPreviewRenderHelper : BlockArrayPreviewRenderHelper() {

    val utils = this as BlockArrayPreviewRenderUtils
    var renderHelperUtils: BlockArrayRenderUtils? = null
    private var status: IntArray? = null
    var work = false
    var key = 0L

    fun startPreview(currentContext: DynamicMachineRenderContext, pos: BlockPos, facing: EnumFacing?): Boolean {
        if (this.startPreview(currentContext)) {
            val lookState = Minecraft.getMinecraft().world.getBlockState(pos)
            val rotate = facing ?: lookState.getValue(BlockController.FACING)
            val moveDir = MiscUtils.rotateYCCWNorthUntil(BlockPos(utils.renderHelperOffset), rotate)
            val newpos = pos.subtract(moveDir)
            utils.matchArray = MiscUtils.rotateYCCWNorthUntil(utils.matchArray, rotate)
            utils.attachedPosition = newpos
            key = pos.toLong()
            renderHelperUtils = utils.renderHelper as BlockArrayRenderUtils
            utils.facing = facing
            initLayers()
            work = true
            return true
        }
        return false
    }

    private fun initLayers() {
        val matchingArray = renderHelperUtils!!.`n$getBlocks`()
        val lowestSlice = matchingArray.min.y
        val maxSlice = matchingArray.max.y
        val status = IntStream.range(lowestSlice, maxSlice + 2).toArray()
        utils.renderedLayer = status[status.size - 1]
        this.status = status
    }

    fun setLayers(newValue: Int) {
        if (renderHelperUtils != null) {
            val size = status!!.size
            val mod = newValue % size

            val value = if (mod < 0) mod + size else mod

            utils.renderedLayer = status!![value]
        }
    }

    fun getLayers(): Int {
        return utils.renderedLayer
    }

    fun renderTranslucentBlocks() {
        utils.`n$renderTranslucentBlocks`()
    }

    fun clear() {
        renderHelperUtils = null
        status = null
        work = false
        key = 0

        if (Minecraft.getMinecraft().player != null) {
            Minecraft.getMinecraft().player.sendMessage(
                TextComponentTranslation(
                    "gui.blueprint.popout.placed.dynamic_pattern.unload"
                )
            )
        }
    }
}