package github.kasuminova.novaeng.common.network;

import github.kasuminova.novaeng.client.gui.GuiEFabricatorController;
import github.kasuminova.novaeng.common.container.data.EFabricatorData;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorController;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorWorker;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class PktEFabricatorGUIData implements IMessage, IMessageHandler<PktEFabricatorGUIData, IMessage> {

    private EFabricatorData data = null;

    public PktEFabricatorGUIData(final EFabricatorController controller) {
        data = new EFabricatorData(
            controller.getLength(),
            controller.getOverclocked(),
            controller.getActiveCooling(),
            controller.getParallelism(),
            controller.getCoolantInputFluids(),
            controller.getCoolantInputCap(),
            controller.getCoolantOutputFluids(),
            controller.getCoolantOutputCap(),
            controller.getEnergyStored(),
            controller.getTotalCrafted(),
            controller.getLevel(),
            workerStatuses(controller.getWorkers())
        );
    }

    private static List<EFabricatorData.WorkerStatus> workerStatuses(final List<EFabricatorWorker> workers) {
        final List<EFabricatorData.WorkerStatus> statuses = new ObjectArrayList<>(workers.size());
        for (EFabricatorWorker worker : workers) {
            final var queue = worker.getQueue();
            final EFabricatorWorker.CraftWork peek = queue.peek();
            statuses.add(new EFabricatorData.WorkerStatus(
                peek != null ? peek.getOutput() : ItemStack.EMPTY, queue.size()));
        }
        return statuses;
    }

    public PktEFabricatorGUIData() {
    }

    @SideOnly(Side.CLIENT)
    protected static void processPacket(final PktEFabricatorGUIData message) {
        EFabricatorData data = message.data;
        GuiScreen cur = Minecraft.getMinecraft().currentScreen;
        if (!(cur instanceof GuiEFabricatorController efGUI)) {
            return;
        }
        if (data == null) {
            return;
        }
        efGUI.onDataUpdate(data);
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        data = EFabricatorData.read(buf);
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        data.write(buf);
    }

    @Override
    public IMessage onMessage(final PktEFabricatorGUIData message, final MessageContext ctx) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            Minecraft.getMinecraft().addScheduledTask(() -> processPacket(message));
        }
        return null;
    }

}
