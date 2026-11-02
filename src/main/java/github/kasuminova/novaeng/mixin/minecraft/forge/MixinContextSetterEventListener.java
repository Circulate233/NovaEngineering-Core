package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.common.util.NovaFastEventListener;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.ASMEventHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.IContextSetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Keeps Forge's active-mod and event context changes while using the ASM handler fast path for non-cancelable events.
 */
@Mixin(targets = "net.minecraftforge.fml.common.eventhandler.EventBus$ContextSetterEventListener", remap = false)
abstract class MixinContextSetterEventListener implements NovaFastEventListener {

    @Shadow
    public abstract ModContainer owner();

    @Shadow
    public abstract ASMEventHandler asm();

    @Override
    public void nova$invokeUncancellable(final Event event) {
        final Loader loader = Loader.instance();
        final ModContainer previous = loader.activeModContainer();
        final ModContainer owner = this.owner();
        loader.setActiveModContainer(owner);
        ((IContextSetter) event).setModContainer(owner);
        ((NovaFastEventListener) this.asm()).nova$invokeUncancellable(event);
        loader.setActiveModContainer(previous);
    }
}
