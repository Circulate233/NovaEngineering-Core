package github.kasuminova.novaeng.mixin.enumcache.harvestcraft;

import com.pam.harvestcraft.blocks.growables.BlockPamSapling.SaplingType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SaplingType.class, remap = false)
public abstract class MixinSaplingType {

    @Shadow
    @Final
    private static SaplingType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SaplingType[] values() {
        return $VALUES;
    }
}
