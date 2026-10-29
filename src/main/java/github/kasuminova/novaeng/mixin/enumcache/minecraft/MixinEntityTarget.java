package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = LootContext.EntityTarget.class, remap = false)
public abstract class MixinEntityTarget {

    @Shadow
    @Final
    private static LootContext.EntityTarget[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static LootContext.EntityTarget[] values() {
        return $VALUES;
    }
}
