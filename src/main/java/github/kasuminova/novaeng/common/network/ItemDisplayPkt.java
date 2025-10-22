package github.kasuminova.novaeng.common.network;

import github.kasuminova.novaeng.NovaEngineeringCore;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemDisplayPkt implements IMessage, IMessageHandler<ItemDisplayPkt, IMessage> {

    @Getter
    private ItemStack stack;
    @Getter
    private ITextComponent name;

    public ItemDisplayPkt() {

    }

    public ItemDisplayPkt(ItemStack stack, EntityPlayer player) {
        this.stack = stack;
        this.name = player.getDisplayName();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        stack = ByteBufUtils.readItemStack(buf);
        name = ITextComponent.Serializer.fromJsonLenient(ByteBufUtils.readUTF8String(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, stack);
        ByteBufUtils.writeUTF8String(buf, ITextComponent.Serializer.componentToJson(name));
    }

    @Override
    public IMessage onMessage(ItemDisplayPkt message, MessageContext ctx) {
        switch (ctx.side) {
            case SERVER -> NovaEngineeringCore.NET_CHANNEL.sendToAll(message);
            case CLIENT -> onClient(message, ctx);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    public void onClient(ItemDisplayPkt message, MessageContext ctx) {
        Minecraft.getMinecraft().player.sendMessage(message.name.appendText(":").appendSibling(message.stack.getTextComponent()));
    }

    @SideOnly(Side.CLIENT)
    public static class TextComponentItemStack extends TextComponentString {

        public TextComponentItemStack(ItemStack stack) {
            super("[" + stack.getDisplayName() + "]");
        }
    }
}