package github.kasuminova.novaeng.client.util

import github.kasuminova.novaeng.mixin.util.BlockArrayPreviewRenderUtils
import github.kasuminova.novaeng.mixin.util.BlockArrayRenderUtils
import hellfirepvp.modularmachinery.client.util.BlockArrayPreviewRenderHelper
import hellfirepvp.modularmachinery.client.util.DynamicMachineRenderContext
import hellfirepvp.modularmachinery.common.block.BlockController
import hellfirepvp.modularmachinery.common.util.MiscUtils
import java.util.stream.IntStream
import net.minecraft.client.Minecraft
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentTranslation

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
            renderHelperUtils = utils.renderHelper as BlockArrayRenderUtils
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

        if (Minecraft.getMinecraft().player != null) {
            Minecraft.getMinecraft().player.sendMessage(
                TextComponentTranslation(
                    "gui.blueprint.popout.placed.dynamic_pattern.unload"
                )
            )
        }
    }

    override fun unloadWorld() {
        super.unloadWorld()
        clear()
    }
}