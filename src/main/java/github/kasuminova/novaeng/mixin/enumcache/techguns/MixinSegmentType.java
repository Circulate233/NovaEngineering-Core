package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.world.dungeon.TemplateSegment.SegmentType;

@Mixin(value = SegmentType.class, remap = false)
public abstract class MixinSegmentType {

    @Shadow
    @Final
    private static SegmentType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SegmentType[] values() {
        return $VALUES;
    }
}
