package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.fml.common.network.FMLEventChannel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FMLEventChannel.EventFactory.class, remap = false)
public abstract class MixinEventFactory {

    @Shadow
    @Final
    private static FMLEventChannel.EventFactory[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FMLEventChannel.EventFactory[] values() {
        return $VALUES;
    }
}
