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
import java.util.stream.IntStream

@Suppress("CAST_NEVER_SUCCEEDS")
object NEWBlockArrayPreviewRenderHelper : BlockArrayPreviewRenderHelper() {

    val utils = this as BlockArrayPreviewRenderUtils
    var renderHelperUtils: BlockArrayRenderUtils? = null
    var status: IntArray? = null
    var work = false

    fun startPreview(currentContext: DynamicMachineRenderContext, pos: BlockPos): Boolean {
        if (this.startPreview(currentContext)) {
            clear()
            val lookState = Minecraft.getMinecraft().world.getBlockState(pos)
            val rotate = lookState.getValue(BlockController.FACING) as EnumFacing
            val moveDir = MiscUtils.rotateYCCWNorthUntil(BlockPos(utils.renderHelperOffset), rotate)
            val newpos = pos.subtract(moveDir)
            utils.matchArray = MiscUtils.rotateYCCWNorthUntil(utils.matchArray, rotate)
            utils.attachedPosition = newpos
            setLayers(0)
            renderHelperUtils = utils.renderHelper as BlockArrayRenderUtils
            work = true
            return true
        }
        return false
    }

    fun setLayers(newValue: Int) {
        if (renderHelperUtils != null) {
            if (status == null) {
                val matchingArray = renderHelperUtils!!.`n$getBlocks`()
                val lowestSlice = matchingArray.min.y
                val maxSlice = matchingArray.max.y
                status = IntStream.range(lowestSlice, maxSlice + 2).toArray()
            }

            var value = newValue

            while (value < 0) {
                value += status!!.size
            }

            value %= status!!.size
            utils.renderedLayer = status!![value]
        }
    }

    fun getLayers(): Int {
        return utils.renderedLayer
    }

    fun renderTranslucentBlocks() {
        utils.`n$renderTranslucentBlocks`()
    }

    override fun tick() {
        super.tick()
        if (work && this.context == null) {
            clear()
        }
    }

    fun clear() {
        renderHelperUtils = null
        status = null
        work = false
        utils.renderedLayer = -1
    }
}