package github.kasuminova.novaeng.common.network;

import appeng.api.AEApi;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.implementations.ContainerPatternEncoder;
import appeng.container.slot.SlotRestrictedInput;
import appeng.items.misc.ItemEncodedPattern;
import appeng.me.GridAccessException;
import appeng.parts.reporting.AbstractPartEncoder;
import com.glodblock.github.util.EmptyMachineSet;
import com.glodblock.github.util.FluidPatternDetails;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorMEChannel;
import github.kasuminova.novaeng.mixin.ae2.AccessorContainerPatternEncoder;
import hellfirepvp.modularmachinery.ModularMachinery;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PktPatternTermUploadPattern implements IMessage, IMessageHandler<PktPatternTermUploadPattern, IMessage> {

    @Override
    public void fromBytes(final ByteBuf buf) {
    }

    @Override
    public void toBytes(final ByteBuf buf) {
    }

    @Override
    public IMessage onMessage(final PktPatternTermUploadPattern message, final MessageContext ctx) {
        final EntityPlayerMP player = ctx.getServerHandler().player;
        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            final Container container = player.openContainer;
            if (!(container instanceof ContainerPatternEncoder encoder)) {
                return;
            }

            final SlotRestrictedInput patternSlotOUT = ((AccessorContainerPatternEncoder) encoder).getPatternSlotOUT();
            final ItemStack patternStack = patternSlotOUT.getStack();
            if (patternStack.isEmpty()) {
                return;
            }

            AbstractPartEncoder part = encoder.getPart();
            IGuiItemObject itemObject = ((AccessorContainerPatternEncoder) encoder).getIGuiItemObject();
            IMachineSet channelNodes;
                if (part != null) {
                    try {
                        channelNodes = part.getProxy().getGrid().getMachines(EFabricatorMEChannel.class);
                    } catch (GridAccessException ignored) {
                        channelNodes = EmptyMachineSet.create(EFabricatorMEChannel.class);
                    }
                } else if (itemObject instanceof IActionHost wirelessTerm) {
                    channelNodes = wirelessTerm.getActionableNode().getGrid().getMachines(EFabricatorMEChannel.class);
                } else {
                    return;
                }

            final IAEItemStack out;
            if (patternStack.getItem() instanceof ItemEncodedPattern item) {
                var pattern = item.getPatternForItem(patternStack, player.world);
                if (pattern.isCraftable() || pattern instanceof FluidPatternDetails) {
                    out = pattern.getCondensedOutputs()[0];
                } else return;
            } else return;

            for (final IGridNode channelNode : channelNodes) {
                EFabricatorMEChannel channel = (EFabricatorMEChannel) channelNode.getMachine();
                for (var patternBus : channel.getController().getPatternBuses()) {
                    if (patternBus.getAePatterns().contains(out)) {
                        player.sendMessage(
                                new TextComponentTranslation(
                                        "novaeng.efabricator_parallel_proc.tooltip.0"
                                )
                        );
                        player.inventory.placeItemBackInInventory(
                                player.world,
                                AEApi.instance().definitions().materials().blankPattern().maybeStack(patternSlotOUT.getStack().getCount()).orElse(ItemStack.EMPTY)
                        );
                        patternSlotOUT.putStack(ItemStack.EMPTY);
                        return;
                    }
                }
            }

            for (final IGridNode channelNode : channelNodes) {
                    EFabricatorMEChannel channel = (EFabricatorMEChannel) channelNode.getMachine();
                    if (channel.insertPattern(patternStack)) {
                        patternSlotOUT.putStack(ItemStack.EMPTY);
                        break;
                    }
                }

        });
        return null;
    }
}