package github.kasuminova.novaeng.client.handler

import github.kasuminova.novaeng.client.util.NEWBlockArrayPreviewRenderHelper
import github.kasuminova.novaeng.common.item.ItemMachineAssemblyTool
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.input.Mouse

@SideOnly(Side.CLIENT)
object MachineAssemblyHandlerClient {

    val mc: Minecraft = Minecraft.getMinecraft()

    @SubscribeEvent
    fun onMouseEvent(event: MouseEvent) {
        if (mc.player != null && mc.player.isSneaking() && NEWBlockArrayPreviewRenderHelper.work) {
            val stack: ItemStack = mc.player.heldItemMainhand
            var delta = -Mouse.getEventDWheel()
            if (delta % 120 == 0) {
                delta /= 120
            }
            if (delta != 0 && stack.item is ItemMachineAssemblyTool) {
                NEWBlockArrayPreviewRenderHelper
                    .setLayers(NEWBlockArrayPreviewRenderHelper.getLayers() + delta)
                event.isCanceled = true
            }
        }
    }

    @SubscribeEvent
    fun tick(event: TickEvent.ClientTickEvent) {
        if (event.phase === TickEvent.Phase.END) {
            return
        }
        NEWBlockArrayPreviewRenderHelper.tick()
    }

    @SubscribeEvent
    fun onRenderLast(event: RenderWorldLastEvent) {
        if (Minecraft.getMinecraft().player == null) return
        NEWBlockArrayPreviewRenderHelper.renderTranslucentBlocks()
    }

    @SubscribeEvent
    fun purgeDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        NEWBlockArrayPreviewRenderHelper.unloadWorld()
    }

    @SubscribeEvent
    fun onWorldChange(unload: WorldEvent.Unload) {
        if (unload.world.isRemote) {
            NEWBlockArrayPreviewRenderHelper.unloadWorld()
        }
    }
}
