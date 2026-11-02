package github.kasuminova.novaeng.mixin.enumcache.modularrouters;

import me.desht.modularrouters.logic.filter.matchers.InspectionMatcher.InspectionSubject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = InspectionSubject.class, remap = false)
public abstract class MixinInspectionSubject {

    @Shadow
    @Final
    private static InspectionSubject[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static InspectionSubject[] values() {
        return $VALUES;
    }
}
