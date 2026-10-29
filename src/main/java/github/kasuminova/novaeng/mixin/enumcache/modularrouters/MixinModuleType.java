package github.kasuminova.novaeng.mixin.enumcache.modularrouters;

import me.desht.modularrouters.item.module.ItemModule.ModuleType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModuleType.class, remap = false)
public abstract class MixinModuleType {

    @Shadow
    @Final
    private static ModuleType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModuleType[] values() {
        return $VALUES;
    }
}
