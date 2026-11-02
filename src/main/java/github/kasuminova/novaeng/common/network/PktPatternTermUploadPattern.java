package github.kasuminova.novaeng.common.network;

import appeng.api.AEApi;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.implementations.ContainerPatternEncoder;
import appeng.items.misc.ItemEncodedPattern;
import appeng.me.GridAccessException;
import com.glodblock.github.util.FluidCraftingPatternDetails;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorMEChannel;
import github.kasuminova.novaeng.mixin.ae2.AccessorContainerPatternEncoder;
import hellfirepvp.modularmachinery.ModularMachinery;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class PktPatternTermUploadPattern implements IMessage, IMessageHandler<PktPatternTermUploadPattern, IMessage> {

    @Override
    public void fromBytes(final ByteBuf buffer) {
    }

    @Override
    public void toBytes(final ByteBuf buffer) {
    }

    @Override
    public IMessage onMessage(final PktPatternTermUploadPattern message, final MessageContext context) {
        final var player = context.getServerHandler().player;
        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            if (!(player.openContainer instanceof ContainerPatternEncoder container)) {
                return;
            }
            final var accessor = (AccessorContainerPatternEncoder) container;
            final var slot = accessor.getPatternSlotOUT();
            final ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                return;
            }
            final var part = container.getPart();
            final var itemObject = accessor.getIGuiItemObject();
            final Iterable<IGridNode> nodes;
            if (part != null) {
                try {
                    nodes = part.getProxy().getGrid().getMachines(EFabricatorMEChannel.class);
                } catch (final GridAccessException ignored) {
                    return;
                }
            } else if (itemObject instanceof IActionHost host) {
                nodes = host.getActionableNode().getGrid().getMachines(EFabricatorMEChannel.class);
            } else {
                return;
            }
            if (!nodes.iterator().hasNext() || !(stack.getItem() instanceof ItemEncodedPattern item)) {
                return;
            }
            final ICraftingPatternDetails pattern = item.getPatternForItem(stack, player.world);
            if (!pattern.isCraftable() && !(pattern instanceof FluidCraftingPatternDetails)) {
                return;
            }
            final IAEItemStack output = pattern.getCondensedOutputs()[0];
            for (final IGridNode node : nodes) {
                final var channel = (EFabricatorMEChannel) node.getMachine();
                final var controller = channel.getController();
                if (controller == null) {
                    continue;
                }
                for (final var bus : controller.getPatternBuses()) {
                    if (bus.getAePatterns().contains(output)) {
                        player.sendMessage(new TextComponentTranslation("novaeng.efabricator_parallel_proc.tooltip.0"));
                        player.inventory.placeItemBackInInventory(player.world, AEApi.instance().definitions().materials()
                                                                                     .blankPattern().maybeStack(slot.getStack().getCount()).orElse(ItemStack.EMPTY));
                        slot.putStack(ItemStack.EMPTY);
                        return;
                    }
                }
            }
            for (final IGridNode node : nodes) {
                if (((EFabricatorMEChannel) node.getMachine()).insertPattern(stack)) {
                    slot.putStack(ItemStack.EMPTY);
                    break;
                }
            }
        });
        return null;
    }
}
