package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.gui.advancements.AdvancementTabType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AdvancementTabType.class, remap = false)
public abstract class MixinAdvancementTabType {

    @Shadow
    @Final
    private static AdvancementTabType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AdvancementTabType[] values() {
        return $VALUES;
    }
}
