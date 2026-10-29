package github.kasuminova.novaeng.mixin.enumcache.thermaldynamics;

import cofh.thermaldynamics.util.ListWrapper.SortType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SortType.class, remap = false)
public abstract class MixinSortType {

    @Shadow
    @Final
    private static SortType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SortType[] values() {
        return $VALUES;
    }
}
