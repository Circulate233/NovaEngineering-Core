package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.entity.monster.EntitySpellcasterIllager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EntitySpellcasterIllager.SpellType.class, remap = false)
public abstract class MixinSpellType {

    @Shadow
    @Final
    private static EntitySpellcasterIllager.SpellType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EntitySpellcasterIllager.SpellType[] values() {
        return $VALUES;
    }
}
