package github.kasuminova.novaeng.mixin.enumcache.konkrete;

import de.keksuccino.konkrete.json.jsonpath.internal.filter.RelationalOperator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RelationalOperator.class, remap = false)
public abstract class MixinRelationalOperator {

    @Shadow
    @Final
    private static RelationalOperator[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RelationalOperator[] values() {
        return $VALUES;
    }
}
