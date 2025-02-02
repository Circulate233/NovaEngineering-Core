package github.kasuminova.novaeng.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class UpdateItemModeMessage implements IMessage {
    private ItemStack stack;
    private int mode;

    public UpdateItemModeMessage() {}

    public UpdateItemModeMessage(ItemStack stack, int mode) {
        this.stack = stack;
        this.mode = mode;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        stack = ByteBufUtils.readItemStack(buf);
        mode = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, stack);
        buf.writeInt(mode);
    }

    public ItemStack getStack() {
        return stack;
    }

    public int getMode() {
        return mode;
    }

    public static class Handler implements IMessageHandler<UpdateItemModeMessage, IMessage>{

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
}