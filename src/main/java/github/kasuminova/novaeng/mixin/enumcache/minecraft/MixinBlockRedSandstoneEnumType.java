package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockRedSandstone;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockRedSandstone.EnumType.class, remap = false)
public abstract class MixinBlockRedSandstoneEnumType {

    @Shadow
    @Final
    private static BlockRedSandstone.EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockRedSandstone.EnumType[] values() {
        return $VALUES;
    }
}
