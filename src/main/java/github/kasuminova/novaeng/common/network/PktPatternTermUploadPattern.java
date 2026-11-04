package github.kasuminova.novaeng.common.network;

import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.networking.security.IActionHost;
import ae2.container.SlotSemantics;
import ae2.container.me.patternencode.ContainerPatternEncodingTerm;
import ae2.core.definitions.AEItems;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorMEChannel;
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
            if (!(player.openContainer instanceof ContainerPatternEncodingTerm container)) {
                return;
            }
            final ItemStack patternStack = container.getSlots(SlotSemantics.ENCODED_PATTERN).stream()
                                                      .findFirst().map(net.minecraft.inventory.Slot::getStack).orElse(ItemStack.EMPTY);
            if (patternStack.isEmpty()) {
                return;
            }
            if (!(container.getTarget() instanceof IActionHost itemObject)) {
                return;
            }
            final var node = itemObject.getActionableNode();
            if (node == null) {
                return;
            }
            final var grid = node.grid();
            if (grid == null) {
                return;
            }
            final var channelNodes = grid.getMachines(EFabricatorMEChannel.class);
            if (channelNodes.isEmpty()) {
                return;
            }
            final IPatternDetails pattern = PatternDetailsHelper.decodePattern(patternStack, player.world);
            if (pattern == null || pattern.getPrimaryOutput() == null) {
                return;
            }
            final var out = pattern.getPrimaryOutput().what();
            for (final EFabricatorMEChannel channel : channelNodes) {
                final var controller = channel.getController();
                if (controller == null) {
                    continue;
                }
                for (final var bus : controller.getPatternBuses()) {
                    if (bus.getAePatterns().contains(out)) {
                        player.sendMessage(new TextComponentTranslation("novaeng.efabricator_parallel_proc.tooltip.0"));
                        player.inventory.placeItemBackInInventory(player.world,
                            new ItemStack(AEItems.BLANK_PATTERN.item(), patternStack.getCount()));
                        container.clear();
                        return;
                    }
                }
            }
            for (final EFabricatorMEChannel channel : channelNodes) {
                if (channel.insertPattern(patternStack)) {
                    container.clear();
                    break;
                }
            }
        });
        return null;
    }
}
