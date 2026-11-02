package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.common.util.TextTable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TextTable.Alignment.class, remap = false)
public abstract class MixinAlignment {

    @Shadow
    @Final
    private static TextTable.Alignment[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TextTable.Alignment[] values() {
        return $VALUES;
    }
}
