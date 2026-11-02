package github.kasuminova.novaeng.mixin.enumcache.ebwizardry;

import electroblob.wizardry.constants.SpellType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SpellType.class, remap = false)
public abstract class MixinSpellType {

    @Shadow
    @Final
    private static SpellType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SpellType[] values() {
        return $VALUES;
    }
}
