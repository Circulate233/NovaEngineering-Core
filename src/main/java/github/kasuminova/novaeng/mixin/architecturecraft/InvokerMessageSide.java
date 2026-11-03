package github.kasuminova.novaeng.mixin.architecturecraft;

import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.network.Message;

import net.minecraftforge.fml.relauncher.Side;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Message.class, remap = false)
public interface InvokerMessageSide {
    @Invoker("getSide")
    Side nova$getMessageSide();
}
