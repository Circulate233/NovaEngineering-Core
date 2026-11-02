package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.tileentity.TileEntityStructure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityStructure.Mode.class, remap = false)
public abstract class MixinTileEntityStructureMode {

    @Shadow
    @Final
    private static TileEntityStructure.Mode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TileEntityStructure.Mode[] values() {
        return $VALUES;
    }
}
