package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.network.play.server.SPacketRecipeBook;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SPacketRecipeBook.State.class, remap = false)
public abstract class MixinSPacketRecipeBookState {

    @Shadow
    @Final
    private static SPacketRecipeBook.State[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SPacketRecipeBook.State[] values() {
        return $VALUES;
    }
}
