package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.fml.common.registry.ItemStackHolderInjector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ItemStackHolderInjector.class, remap = false)
public abstract class MixinItemStackHolderInjector {

    @Shadow
    @Final
    private static ItemStackHolderInjector[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ItemStackHolderInjector[] values() {
        return $VALUES;
    }
}
