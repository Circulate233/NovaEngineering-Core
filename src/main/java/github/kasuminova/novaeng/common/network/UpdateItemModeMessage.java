package github.kasuminova.novaeng.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

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
}