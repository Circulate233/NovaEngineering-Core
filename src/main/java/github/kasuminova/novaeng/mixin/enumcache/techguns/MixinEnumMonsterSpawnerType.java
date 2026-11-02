package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.blocks.EnumMonsterSpawnerType;

@Mixin(value = EnumMonsterSpawnerType.class, remap = false)
public abstract class MixinEnumMonsterSpawnerType {

    @Shadow
    @Final
    private static EnumMonsterSpawnerType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumMonsterSpawnerType[] values() {
        return $VALUES;
    }
}
