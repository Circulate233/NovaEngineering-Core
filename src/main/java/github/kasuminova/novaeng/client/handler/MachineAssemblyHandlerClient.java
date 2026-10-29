package github.kasuminova.novaeng.client.handler;

import github.kasuminova.novaeng.client.util.NEWBlockArrayPreviewRenderHelper;
import github.kasuminova.novaeng.common.item.ItemMachineAssemblyTool;
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public final class MachineAssemblyHandlerClient {
    public static final MachineAssemblyHandlerClient INSTANCE = new MachineAssemblyHandlerClient();

    private MachineAssemblyHandlerClient() {
    }

    public Minecraft getMc() {
        return Minecraft.getMinecraft();
    }

    @SubscribeEvent
    public void addMachineCtrlTooltip(final ItemTooltipEvent event) {
        final var stack = event.getItemStack();
        if (stack.getItem() instanceof ItemBlock item) {
            final var machine = NEWMachineAssemblyManager.getDynamicMachine(item.getBlock(), stack.getMetadata());
            if (machine == null) {
                return;
            }
            event.getToolTip().add(I18n.format("tooltip.machine_assembly_tool.0", machine.getLocalizedName()));
            event.getToolTip().add(I18n.format("tooltip.machine_assembly_tool.1", ItemMachineAssemblyTool.INSTANCE.getItemStackDisplayName(ItemStack.EMPTY)));
        }
    }

    @SubscribeEvent
    public void onMouseEvent(final MouseEvent event) {
        final var mc = this.getMc();
        final var preview = NEWBlockArrayPreviewRenderHelper.INSTANCE;
        if (mc.player != null && mc.player.isSneaking() && preview.getWork()) {
            final var stack = mc.player.getHeldItemMainhand();
            int delta = -Mouse.getEventDWheel();
            if (delta % 120 == 0) {
                delta /= 120;
            }
            if (delta != 0 && stack.getItem() instanceof ItemMachineAssemblyTool) {
                preview.setLayers(preview.getLayers() + delta);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void tick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            NEWBlockArrayPreviewRenderHelper.INSTANCE.tick();
        }
    }

    @SubscribeEvent
    public void onRenderLast(final RenderWorldLastEvent event) {
        if (Minecraft.getMinecraft().player != null) {
            NEWBlockArrayPreviewRenderHelper.INSTANCE.renderTranslucentBlocks();
        }
    }

    @SubscribeEvent
    public void purgeDisconnect(final FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        NEWBlockArrayPreviewRenderHelper.INSTANCE.unloadWorld();
    }

    @SubscribeEvent
    public void onWorldChange(final WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            NEWBlockArrayPreviewRenderHelper.INSTANCE.unloadWorld();
        }
    }
}
