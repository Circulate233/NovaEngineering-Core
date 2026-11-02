package github.kasuminova.novaeng.mixin.enumcache.mets;

import net.lrsoft.mets.block.tileentity.LargeFabricator.IFabricatorPart.FabricatorType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FabricatorType.class, remap = false)
public abstract class MixinFabricatorType {

    @Shadow
    @Final
    private static FabricatorType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FabricatorType[] values() {
        return $VALUES;
    }
}
