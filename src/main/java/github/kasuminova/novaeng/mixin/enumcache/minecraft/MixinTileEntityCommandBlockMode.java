package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.tileentity.TileEntityCommandBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityCommandBlock.Mode.class, remap = false)
public abstract class MixinTileEntityCommandBlockMode {

    @Shadow
    @Final
    private static TileEntityCommandBlock.Mode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TileEntityCommandBlock.Mode[] values() {
        return $VALUES;
    }
}
