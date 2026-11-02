package github.kasuminova.novaeng.mixin.enumcache.modularrouters;

import me.desht.modularrouters.logic.compiled.CompiledPlayerModule.Section;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Section.class, remap = false)
public abstract class MixinSection {

    @Shadow
    @Final
    private static Section[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Section[] values() {
        return $VALUES;
    }
}
