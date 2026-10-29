package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockDoor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockDoor.EnumDoorHalf.class, remap = false)
public abstract class MixinEnumDoorHalf {

    @Shadow
    @Final
    private static BlockDoor.EnumDoorHalf[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockDoor.EnumDoorHalf[] values() {
        return $VALUES;
    }
}
