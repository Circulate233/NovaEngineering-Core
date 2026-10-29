package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.settings.KeyConflictContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = KeyConflictContext.class, remap = false)
public abstract class MixinKeyConflictContext {

    @Shadow
    @Final
    private static KeyConflictContext[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static KeyConflictContext[] values() {
        return $VALUES;
    }
}
