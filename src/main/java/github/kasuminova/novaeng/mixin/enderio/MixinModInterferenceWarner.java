package github.kasuminova.novaeng.mixin.enderio;

import crazypants.enderio.base.diagnostics.ModInterferenceWarner;
import crazypants.enderio.base.events.EnderIOLifecycleEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ModInterferenceWarner.class, remap = false)
public class MixinModInterferenceWarner {

    /**
     * @author circulaiton
     * @reason 我不需要有人告诉我不要用opt
     */
    @SubscribeEvent
    @Overwrite
    public static void onEvent(EnderIOLifecycleEvent.ServerAboutToStart.Pre event) {

    }
}
