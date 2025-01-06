package github.kasuminova.novaeng.common.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class UpdateItemModeMessageHandler implements IMessageHandler<github.kasuminova.novaeng.common.network.UpdateItemModeMessage, IMessage> {

    @Override
    public IMessage onMessage(github.kasuminova.novaeng.common.network.UpdateItemModeMessage message, MessageContext ctx) {
        EntityPlayer player = ctx.getServerHandler().player;
        if (player == null) {
            return null;
        }

        if (!player.world.isRemote){
            ItemStack stack = player.getHeldItem(EnumHand.MAIN_HAND);
            if (stack.getItem() == message.getStack().getItem()) {
                if (stack.getTagCompound() != null) {
                    stack.getTagCompound().setInteger("mode", message.getMode());
                }
            }
        }

        return null;
    }
}